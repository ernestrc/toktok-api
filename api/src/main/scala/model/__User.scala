package model

import com.novus.salat._
import org.joda.time.DateTime
import org.json4s

case class __User(
  id: Option[Int],
  username: String,
  email: String,
  passwordHash: String,
  created: Option[DateTime]
) {
  @transient lazy val toJson: json4s.JObject = __User.userGrater.toJSON(this)
}

object __User {
  import com.novus.salat.global._
  implicit val userGrater: Grater[__User] = grater[__User]
}