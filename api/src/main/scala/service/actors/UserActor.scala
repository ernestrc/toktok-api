package service.actors

import akka.actor.SupervisorStrategy.{Stop, Restart}
import akka.actor._
import akka.event.LoggingAdapter
import akka.pattern.pipe
import com.mongodb.casbah.MongoClient
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.global._
import config.GlobalConfig
import dal.MongoSource
import model.Exceptions.UserExistsException
import model.{SID, Receipt}
import org.bson.types.ObjectId
import service.{Command, Event}
import unstable.macros.Macros._
import unstable.macros.TypeHint
import utils.Implicits._
import akka.pattern.ask

import scala.concurrent.Future

@Salat
sealed trait UserCommand extends Command
case class CreateUserCommand(username: String, passwordHash: String, email: String) extends UserCommand
case class ChangeUserPasswordCommand(override val entityId: String, newPassHash: String, oldPassHash: String) extends UserCommand


@Salat
sealed trait UserEvent extends Event
case class CreateUserAnchor(@Key("_id") uuid: Option[ObjectId], username: String, passwordHash: String, email: String) extends UserEvent with Event
case class PasswordChangedEvent(entityId: SID, newPassword: String) extends UserEvent with Event
case class UserActivatedEvent(entityId: SID) extends UserEvent with Event

object UserActor {
  val eventSerializers: PartialFunction[TypeHint, Grater[_ <: UserEvent]] = grateSealed[UserEvent]
}

/**
 *
 * @param anchor
 * @param source
 */
class UserActor(anchor: CreateUserAnchor, val source: MongoSource[UserEvent]) extends EventSourcedActor[UserEvent] {

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.info(s"Booting up UserActor - ${self.path.name}...")
    val count = source.findAllByEntityId(entityId).foldLeft(0) { (cc, ev) ⇒ applyEvent(ev); cc + 1}
    log.info(s"Finished booting up UserActor - ${self.path.name}. Applied $count events")
  }

  val entityId: SID = anchor.uuid.get.toSid
  val username: String = anchor.username
  var password: String = anchor.passwordHash
  var email: String = anchor.passwordHash
  var activated: Boolean = true
  //TODO implement emailer
  var blacklist: Boolean = false


  override def applyEvent: PartialFunction[UserEvent, Unit] = {
    case _: UserActivatedEvent ⇒ activated = true
    case PasswordChangedEvent(uuid, pass) ⇒ password = pass
  }

  override def processCommand = PartialFunction.apply[Any, UserEvent]{
    case ChangeUserPasswordCommand(uuid, newPassHash, oldPassHash) ⇒ val xword = password; oldPassHash match {
      case `xword` ⇒ PasswordChangedEvent(uuid, newPassHash)
      case anyElse ⇒ throw new Exception("Wrong password!")
    }
  }
}

/**
 *
 */
class UsersGuardian extends Actor with ActorLogging {

  import context.dispatcher

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(loggingEnabled = true) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Restart
    case _: DeathPactException => Restart
    case _: Exception => Restart
  }

  override def preStart(): Unit = {
    log.info(s"CommandSideDao$$Users is up and running in path ${self.path}")
    source.findAllEventsOfType[CreateUserAnchor].foreach { anchor ⇒
      log.info(s"Booting up user actor for user ${anchor.username}")
      addUser(anchor)
      context.actorOf(Props(classOf[UserActor], anchor, source), anchor.uuid.get.toString)
    }
  }

  val client = MongoClient(GlobalConfig.mongoHost)
  val db = client(GlobalConfig.mongoDb)
  val serializers: PartialFunction[TypeHint, Grater[_ <: UserEvent]] = UserActor.eventSerializers
  implicit val logger: LoggingAdapter = log
  val source = new MongoSource[UserEvent](db, serializers)

  def addUser(anchor: CreateUserAnchor): Unit = {
    users += (anchor.uuid.get → anchor.username)
  }

  val users = scala.collection.mutable.Set.empty[(ObjectId, String)]

  def createNewUserCommand(username: String, pass: String, email: String): Future[Receipt] = Future {
    if (users.exists(_._2 == username))
      throw UserExistsException(username)
    else {
      val anchor = CreateUserAnchor(None, username, pass, email)
      val id = Some(source.save(anchor).get.toObjectId)
      anchor.copy(uuid = id)
    }
  }.map { anchor: CreateUserAnchor ⇒
    addUser(anchor)
    val msg = s"Successfully created user $username"
    context.actorOf(Props(classOf[UserActor], anchor, source), anchor.uuid.get.toString)
    log.info(msg)
    Receipt(success = true, updated = anchor.uuid.get.toSid, message = msg)
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
    val uuid = cmd.entityId
    context.stop(context.child(uuid.toString).get)
    log.warning(s"Actor of $uuid was unresponsive so it was stopped!")
    val anchor: CreateUserAnchor = source.findOneByObjectId[CreateUserAnchor](uuid.toObjectId).get
    val reborn = context.actorOf(Props(classOf[UserActor], anchor, source), uuid)
    reborn.tell(cmd, requester)
  }.recover {
    case err: Exception ⇒ requester ! Receipt.error(err, "Unable to process command!")
  }

  override def receive: Receive = {
    case CreateUserCommand(username, pass, email) ⇒
      createNewUserCommand(username, pass, email) pipeTo sender()
    case cmd: UserCommand ⇒ forwardToChildOrCreateNew(cmd, sender())
    case anyElse ⇒ log.error(s"Oops, it looks like I shouldn't have received $anyElse")
  }

}