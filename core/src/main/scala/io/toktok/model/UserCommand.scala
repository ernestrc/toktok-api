package io.toktok.model

import com.novus.salat.annotations.Salat
import krakken.model.{SID, Command}

@Salat
sealed trait UserCommand extends Command

case class ForgotPasswordCommand(username: String, email: String) extends UserCommand

case class CreateUserCommand(username: String, password: String, email: String) extends UserCommand

case class ChangeUserPasswordCommand(override val entityId: SID, newPass: String, oldPass: String) extends UserCommand

case class ActivateUserCommand(override val entityId: SID) extends UserCommand