package io.toktok.model

import krakken.model.{View, SID}
import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._

sealed trait UserView extends View
case class UsersList(users: List[SID]) extends UserView
object UsersList{
  implicit val usersListJsonFormat: JsonFormat[UsersList] = jsonFormat1(UsersList.apply)
}