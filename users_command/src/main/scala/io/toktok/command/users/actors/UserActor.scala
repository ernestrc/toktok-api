package io.toktok.command.users.actors

import java.util.NoSuchElementException

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor._
import akka.event.LoggingAdapter
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.mongodb.casbah.MongoClient
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.global._
import io.toktok.command.users.ServiceConfig
import io.toktok.dal.MongoSource
import io.toktok.model.Exceptions.{UserExistsException, WrongPasswordException}
import io.toktok.model._
import io.toktok.service.EventSourcedActor
import io.toktok.utils.Implicits._
import org.bson.types.ObjectId
import unstable.macros.Macros._
import unstable.macros.TypeHint
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.Future
import scala.util.{Failure, Success}

@Salat
sealed trait UserCommand extends Command
case class CreateUserCommand(username: String, password: String, email: String) extends UserCommand
case class ChangeUserPasswordCommand(override val entityId: SID, newPass: String, oldPass: String) extends UserCommand
case class ForgotPasswordCommand(username: String, email: String) extends UserCommand
case class ActivateUserCommand(override val entityId: SID) extends UserCommand


@Salat
sealed trait UserEvent extends Event
case class UserCreatedAnchor(@Key("_id") uuid: Option[ObjectId], username: String, passwordHash: String, email: String) extends UserEvent with Event
case class PasswordChangedEvent(entityId: SID, newPassword: String) extends UserEvent with Event
case class UserActivatedEvent(entityId: SID) extends UserEvent with Event
case class SendNewPasswordEvent(entityId: SID, newPassword: SID) extends UserEvent with Event

object UserActor {
  val eventSerializers: PartialFunction[TypeHint, Grater[_ <: UserEvent]] = grateSealed[UserEvent]
}

/**
 *
 * @param anchor
 * @param source
 */
class UserActor(anchor: UserCreatedAnchor, val source: MongoSource[UserEvent]) extends EventSourcedActor[UserEvent] {

  implicit val system: ActorSystem = context.system

  implicit val entityId: Option[SID] = anchor.uuid.map(_.toSid)
  val username: String = anchor.username
  var password: String = anchor.passwordHash
  var email: String = anchor.email
  var activated: Boolean = true //TODO implement emailer

  val subscriptions: List[Subscription] = List.empty

  override val eventProcessor: PartialFunction[Event, Unit] = {
    case _: UserActivatedEvent ⇒
      log.info(s"User $username has been activated")
      activated = true
    case PasswordChangedEvent(uuid, pass) ⇒
      log.info(s"User $username changed password to $pass")
      password = pass
    case SendNewPasswordEvent(uuid, pass) ⇒
      log.info(s"New password for user $username should be on its way")
  }

  override val commandProcessor: PartialFunction[Command, List[UserEvent]] = {
    case ActivateUserCommand(id) ⇒ UserActivatedEvent(id) :: Nil
    case ForgotPasswordCommand(user, mail) ⇒ val em = email;
      mail match {
        case `em` ⇒ val newPass = ObjectId.get().toString
          SendNewPasswordEvent(entityId.get, newPass) :: PasswordChangedEvent(entityId.get, newPass) :: Nil
        case anyElse ⇒
          val msg = "Wrong email and username combination!"
          log.debug(msg + s" $mail did not match $em")
          throw new Exception(msg)
      }
    case ChangeUserPasswordCommand(uuid, newPass, oldPass) ⇒
      if (BCrypt.checkpw(oldPass, password)) {
        PasswordChangedEvent(uuid, BCrypt.hashpw(newPass, BCrypt.gensalt())) :: Nil
      } else throw new WrongPasswordException
  }
}

/**
 *
 */
class UsersCommandSideActor extends Actor with ActorLogging {

  import context.dispatcher

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(loggingEnabled = true) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Restart
    case _: DeathPactException => Restart
    case _: Exception => Restart
  }

  override def preStart(): Unit = {
    log.info(s"CommandSideDao$$Users is up and running in path ${self.path}")
    source.findAllEventsOfType[UserCreatedAnchor].foreach { anchor ⇒
      log.info(s"Booting up user actor for user ${anchor.username}")
      addUser(anchor)
      context.actorOf(Props(classOf[UserActor], anchor, source), anchor.uuid.get.toString)
    }
    log.info(s"Users' actors: $users")
  }

  implicit val timeout: Timeout = ServiceConfig.ACTOR_TIMEOUT
  implicit val logger: LoggingAdapter = log
  val client = MongoClient(ServiceConfig.mongoHost)
  val db = client(ServiceConfig.mongoDb)
  val serializers: PartialFunction[TypeHint, Grater[_ <: UserEvent]] = UserActor.eventSerializers
  val source = new MongoSource[UserEvent](db, serializers)

  def addUser(anchor: UserCreatedAnchor): Unit = {
    users += (anchor.uuid.get → anchor.username)
  }

  val users = scala.collection.mutable.Set.empty[(ObjectId, String)]

  def createNewUserCommand(username: String, pass: String, email: String): Future[Receipt] = Future {
    if (users.exists(_._2 == username))
      throw UserExistsException(username)
    else {
      val passHash = BCrypt.hashpw(pass, BCrypt.gensalt())
      val anchor = UserCreatedAnchor(None, username, passHash, email)
      val id = Some(source.save(anchor).get.toObjectId)
      anchor.copy(uuid = id)
    }
  }.map { anchor: UserCreatedAnchor ⇒
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
  }.andThen {
    //auto activate white-listed
    case Success(rec) if rec.success ⇒
      val uuid = rec.updated
      if (ServiceConfig.WHITELIST_EMAIL.exists(email.matches)) {
        val selection = context.system.actorSelection(context.system / self.path.name / uuid)
        selection.resolveOne().map { c ⇒
          log.info(s"User's \'$username\' email \'$email\' is whitelisted. Activating user now!")
          c.ask(ActivateUserCommand(uuid))
        }.andThen {
          case Failure(err) ⇒ log.error("Could not activate whitelisted user!")
        }
      }
      else log.info(s"User's \'$username\' email \'$email\' not in whitelist")
  }

  def forwardToChildOrCreateNew(cmd: Command, requester: ActorRef): Future[Unit] = Future {
    val uuid = cmd.entityId
    context.stop(context.child(uuid.toString).get)
    log.warning(s"Actor of $uuid was unresponsive so it was stopped!")
    val anchor: UserCreatedAnchor = source.findOneByObjectId[UserCreatedAnchor](uuid.toObjectId).get
    val reborn = context.actorOf(Props(classOf[UserActor], anchor, source), uuid)
    reborn.tell(cmd, requester)
  }.recover {
    case err: Exception ⇒ requester ! Receipt.error(err, "Unable to process command!")
  }

  def forgotPasswordCommand(cmd: ForgotPasswordCommand): Future[Receipt] = Future {
    val user = users.find(_._2 == cmd.username).get
    context.child(user._1.toString).get
  }.flatMap { userActor ⇒
    userActor.ask(cmd)(ServiceConfig.ACTOR_TIMEOUT).mapTo[Receipt]
  }.recover {
    case ex: NoSuchElementException ⇒
      Receipt(success = false, message = "Username doesn't correspond to any user")
  }

  override def receive: Receive = {
    case cmd: ForgotPasswordCommand ⇒ forgotPasswordCommand(cmd) pipeTo sender()
    case CreateUserCommand(username, pass, email) ⇒
      createNewUserCommand(username, pass, email) pipeTo sender()
    case cmd: UserCommand ⇒ forwardToChildOrCreateNew(cmd, sender())
    case anyElse ⇒ log.error(s"Oops, it looks like I shouldn't have received $anyElse")
  }

}