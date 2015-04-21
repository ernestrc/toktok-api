package io.toktok.model

import krakken.model.{View, SID}
import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._

sealed trait UserView extends View
case class User(id: SID, username: String) extends UserView
object User{
  implicit val userJsonFormat: JsonFormat[User] = jsonFormat2(User.apply)
}
case class UsernamesList(users: List[SID]) extends UserView
object UsernamesList{
  implicit val usersListJsonFormat: JsonFormat[UsernamesList] = jsonFormat1(UsernamesList.apply)
}
case class UsersList(users: List[User]) extends UserView
object UsersList{
  implicit val usersListJsonFormat: JsonFormat[UsersList] = jsonFormat1(UsersList.apply)
}