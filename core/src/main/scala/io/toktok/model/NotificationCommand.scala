package io.toktok.model

import krakken.model.{Command, SID}

trait NotificationCommand extends Command

case class SendActivationEmailCommand(userId: SID, username: String, email: String) extends NotificationCommand

case class SendNewPasswordCommand(userId: SID, newPass: String, email: String) extends NotificationCommand

