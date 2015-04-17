package io.toktok.model

import krakken.model.{View, SID}
import spray.json.DefaultJsonProtocol._
import spray.json.JsonFormat

sealed trait SessionView extends View
case class UserSession(userId: SID, sessionId: SID) extends SessionView
object UserSession{
  implicit val userSessionJsonFormat: JsonFormat[UserSession] =
    jsonFormat2(UserSession.apply)
}