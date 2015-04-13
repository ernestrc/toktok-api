package io.toktok.command.users

import akka.actor.Props
import io.toktok.command.users.actors._
import krakken.MicroService

object Boot extends App {

  val actors = Props[UserCommandGuardian] :: Props[SessionCommandGuardian] :: Nil
  
  MicroService("users_command", actors)

}