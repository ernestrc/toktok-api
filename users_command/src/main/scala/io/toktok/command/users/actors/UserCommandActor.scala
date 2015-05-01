package io.toktok.command.users.actors

import akka.actor._
import io.toktok.command.users.Exceptions.{WrongEmailPasswordException, UserAlreadyActivatedException, WrongPasswordException}
import io.toktok.model._
import krakken.dal.MongoSource
import krakken.model._
import krakken.system.EventSourcedCommandActor
import krakken.utils.Implicits._
import org.bson.types.ObjectId
import org.mindrot.jbcrypt.BCrypt

class UserCommandActor(anchor: UserCreatedAnchor) extends EventSourcedCommandActor[UserEvent] {

  implicit val system: ActorSystem = context.system

  implicit val entityId: Option[SID] = anchor.uuid.map(_.toSid)
  val username: String = anchor.username
  var password: String = anchor.passwordHash
  var email: String = anchor.email
  var activated: Boolean = false

  val subscriptions: List[Subscription] = List.empty

  override val eventProcessor: PartialFunction[Event, Unit] = {
    case _: UserActivatedEvent ⇒
      log.info(s"User $username has been activated")
      activated = true
    case PasswordChangedEvent(uuid, pass) ⇒
      log.info(s"User $username changed password to $pass")
      password = pass
    case SendNewPasswordEvent(uuid, pass, em) ⇒
      log.info(s"New password for user $username should be on its way")
  }

  override val commandProcessor: PartialFunction[Command, List[UserEvent]] = {
    case ActivateUserCommand(id) ⇒
      if(!activated) UserActivatedEvent(id) :: Nil
      else throw new UserAlreadyActivatedException
    case ForgotPasswordCommand(user, mail) ⇒ val em = email;
      mail match {
        case `em` ⇒ val newPass = ObjectId.get().toString
          SendNewPasswordEvent(entityId.get, newPass, email) ::
            PasswordChangedEvent(entityId.get, BCrypt.hashpw(newPass, BCrypt.gensalt())) :: Nil
        case anyElse ⇒
          log.debug(s"Wrong email and username combination! $mail did not match $em")
          throw new WrongEmailPasswordException
      }
    case ChangeUserPasswordCommand(uuid, newPass, oldPass) ⇒
      if (BCrypt.checkpw(oldPass, password)) {
        PasswordChangedEvent(uuid, BCrypt.hashpw(newPass, BCrypt.gensalt())) :: Nil
      } else throw new WrongPasswordException
  }
}