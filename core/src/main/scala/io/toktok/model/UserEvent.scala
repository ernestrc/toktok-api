package io.toktok.model

import com.novus.salat.annotations.{Key, Salat}
import krakken.model.{Event, SID}
import org.bson.types.ObjectId

@Salat
sealed trait UserEvent extends Event

case class UserCreatedAnchor(@Key("_id") uuid: Option[ObjectId], username: String, passwordHash: String, email: String) extends UserEvent with Event

case class UserActivatedEvent(entityId: SID) extends UserEvent with Event

case class PasswordChangedEvent(entityId: SID, newPassword: String) extends UserEvent with Event

case class SendNewPasswordEvent(entityId: SID, newPassword: SID) extends UserEvent with Event
