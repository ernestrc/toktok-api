package model

/**
 * Created by ernest on 4/2/15.
 */
object Exceptions {

  case class UserExistsException(username: String) extends Exception(s"User $username already exists")

}
