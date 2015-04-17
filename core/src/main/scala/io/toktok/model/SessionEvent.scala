package io.toktok.model

import com.novus.salat.annotations.Salat
import krakken.model._
import spray.json.DefaultJsonProtocol._
import spray.json.JsonFormat

@Salat
sealed trait SessionEvent extends Event

case class TokenCreatedEvent(sessionId: String, token: String) extends SessionEvent

object TokenCreatedEvent {
  implicit val tokenCreatedEventJsonFormat: JsonFormat[TokenCreatedEvent] =
    jsonFormat2(TokenCreatedEvent.apply)
}

case class SessionCreatedAnchor(userId: SID, opentokSessionId: SID) extends SessionEvent