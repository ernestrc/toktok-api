package io.toktok.command.users.actors

import akka.actor._
import io.toktok.command.users.Exceptions.WrongPasswordException
import io.toktok.model._
import krakken.dal.MongoSource
import krakken.model._
import krakken.service.EventSourcedActor
import krakken.utils.Implicits._
import org.bson.types.ObjectId
import org.mindrot.jbcrypt.BCrypt

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