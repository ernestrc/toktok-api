package io.toktok.model

import com.novus.salat.annotations.Salat
import krakken.model.{SID, Event}

@Salat
sealed trait SessionEvent extends Event

case class TokenCreatedEvent(sessionId: String, token: String) extends SessionEvent

case class SessionCreatedAnchor(userId: SID, opentokSessionId: SID) extends SessionEvent