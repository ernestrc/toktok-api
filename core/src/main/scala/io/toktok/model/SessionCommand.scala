package io.toktok.model

import com.novus.salat.annotations.Salat
import krakken.model.{Command, SID}

@Salat
sealed trait SessionCommand extends Command

case class GenerateSessionCommand(userId: SID) extends SessionCommand

case class GenerateTokenCommand(userId: SID) extends SessionCommand