package io.toktok.command.users

import krakken.model.Exceptions.KrakkenException

/**
 * Created by ernest on 4/2/15.
 */
object Exceptions {

  case class UserExistsException(username: String) extends KrakkenException(s"User $username already exists")
  class WrongPasswordException extends KrakkenException("Wrong password!")
  class UserAlreadyActivatedException extends KrakkenException("User already activated!")
  class WrongEmailPasswordException extends KrakkenException("Wrong email and username combination!")

}
