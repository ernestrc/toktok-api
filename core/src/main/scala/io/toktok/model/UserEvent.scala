package io.toktok.model

import com.novus.salat.annotations.{Key, Salat}
import krakken.macros.Macros._
import krakken.model._
import org.bson.types.ObjectId
import spray.json.DefaultJsonProtocol._
import spray.json.JsonFormat
import com.novus.salat.global.ctx

@Salat
sealed trait UserEvent extends Event

case class UserCreatedAnchor(@Key("_id") uuid: Option[ObjectId], username: String, passwordHash: String, email: String) extends UserEvent with Event

case class UserActivatedEvent(entityId: SID) extends UserEvent with Event

case class PasswordChangedEvent(entityId: SID, newPassword: String) extends UserEvent with Event
object PasswordChangedEvent{
  implicit val passwordChangedEventFormat: JsonFormat[PasswordChangedEvent] = jsonFormat2(PasswordChangedEvent.apply)
}

case class SendNewPasswordEvent(entityId: SID, newPassword: SID) extends UserEvent with Event
