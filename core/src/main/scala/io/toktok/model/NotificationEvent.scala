package io.toktok.model

import krakken.model.{SID, Event}

sealed trait NotificationEvent extends Event
case class ActivationEmailSendEvent(entityId: SID, email: String) extends NotificationEvent
case class NewPasswordEmailSent(entityId: SID) extends NotificationEvent

