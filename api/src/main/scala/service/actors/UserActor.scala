package service.actors

import akka.actor.SupervisorStrategy.{Stop, Restart}
import akka.actor._
import akka.pattern.pipe
import com.mongodb.casbah.MongoClient
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.global._
import config.GlobalConfig
import dal.MongoSource
import model.Exceptions.UserExistsException
import model.Receipt
import org.bson.types.ObjectId
import service.{Command, Event}
import unstable.macros.Macros._
import unstable.macros.TypeHint
import utils.Implicits._
import akka.pattern.ask

import scala.concurrent.Future

sealed trait UserCommand extends Command
case class CreateUserCommand(username: String, passwordHash: String, email: String) extends UserCommand
case class ChangeUserPasswordCommand(override val uuid: Option[ObjectId], newPassHash: String, oldPassHash: String) extends UserCommand


@Salat
sealed trait UserEvent extends Event
case class CreateUserEvent(@Key("_id") _id: Option[ObjectId], username: String, passwordHash: String, email: String) extends UserEvent with Event
case class UserActivatedEvent(uid: ObjectId)

object UserActor {
  val eventSerializers: PartialFunction[TypeHint, Grater[_ <: UserEvent]] = grateSealed[UserEvent]
}

// TODO FIND SOURCE OF : java.lang.RuntimeException: in: unexpected OID input class='org.json4s.JsonAST$JString'
class UserActor(anchor: CreateUserEvent, source: MongoSource[UserEvent]) extends EventSourcedActor {

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.info(s"Booting up UserActor - ${self.path.name}...")
    val count = source.findByEntityId(id).foldLeft(0) { (cc, ev) ⇒ applyEvent(ev); cc + 1}
    log.info(s"Finished booting up UserActor - ${self.path.name}. Applied $count events")
  }

  val id: ObjectId = anchor._id.get
  val username: String = anchor.username
  var password: String = anchor.passwordHash
  var email: String = anchor.passwordHash
  var activated: Boolean = true
  //TODO implement emailer
  var blacklist: Boolean = false


  override def applyEvent: PartialFunction[UserEvent, Unit] = {
    case _: UserActivatedEvent ⇒ activated = true
    case _ ⇒
  }

  override def processCommand: Receive = {
    case ChangeUserPasswordCommand(uuid, newPassHash, oldPassHash) if uuid.get == id ⇒

  }
}

class UsersGuardian extends Actor with ActorLogging {

  import context.dispatcher

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(loggingEnabled = true) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Restart
    case _: DeathPactException => Restart
    case _: Exception => Restart
  }

  def addUser(anchor: CreateUserEvent): Unit = {
    users += (anchor._id.get → anchor.username)
  }

  override def preStart(): Unit = {
    log.info(s"CommandSideDao$$Users is up and running in path ${self.path}")
    source.findAllEventsOfType[CreateUserEvent].foreach { anchor ⇒
      log.info(s"Booting up user actor for user ${anchor.username}")
      addUser(anchor)
      context.actorOf(Props(classOf[UserActor], anchor, source), anchor._id.get.toString)
    }
  }

  val client = MongoClient(GlobalConfig.mongoHost)
  val db = client(GlobalConfig.mongoDb)
  val serializers: PartialFunction[TypeHint, Grater[_ <: UserEvent]] = UserActor.eventSerializers
  val source = new MongoSource[UserEvent](db, serializers)

  val users = scala.collection.mutable.Set.empty[(ObjectId, String)]

  def createNewUserCommand(username: String, pass: String, email: String): Future[Receipt] = Future {
    if (users.exists(_._2 == username))
      throw UserExistsException(username)
    else {
      val anchor = CreateUserEvent(None, username, pass, email)
      val id = source.save(anchor).get
      anchor.copy(_id = Some(id))
    }
  }.map { anchor: CreateUserEvent ⇒
    addUser(anchor)
    val msg = s"Successfully created user $username"
    context.actorOf(Props(classOf[UserActor], anchor, source), anchor._id.get.toString)
    log.info(msg)
    Receipt(success = true, updated = anchor._id, message = msg)
  }.recover {
    case ex@UserExistsException(user) ⇒
      val msg = s"Could not create user $user because it already exists!"
      log.debug(msg)
      Receipt.error(ex, msg)
    case err: Exception ⇒
      val msg = s"Could not create user $username"
      log.error(err, msg)
      Receipt.error(err, msg)
  }

  def forwardToChildOrCreateNew(cmd: Command, requester: ActorRef): Future[Unit] = Future {
    val uuid = cmd.uuid.get
    context.stop(context.child(uuid.toString).get)
    log.warning(s"Actor of $uuid was unresponsive so it was stopped!")
    val anchor = source.findOneById[CreateUserEvent](uuid)
    val reborn = context.actorOf(Props(classOf[UserActor], anchor, source), anchor.get.toString)
    reborn.tell(cmd, requester)
  }.recover{
    case err: Exception ⇒ requester ! Receipt.error(err, "Unable to process command!")
  }

  override def receive: Receive = {
    case CreateUserCommand(username, pass, email) ⇒
      createNewUserCommand(username, pass, email) pipeTo sender()
    case cmd: UserCommand ⇒ forwardToChildOrCreateNew(cmd, sender())
    case anyElse ⇒ log.error(s"Oops, it looks like I shouldn't have received $anyElse")
  }

}