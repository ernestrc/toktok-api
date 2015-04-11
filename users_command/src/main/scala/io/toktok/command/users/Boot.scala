package io.toktok.command.users

import akka.actor.Props
import io.toktok.command.users.actors._
import krakken.MicroService

object Boot extends App {

  val actors = Props[UserGuardian] :: Props[SessionGuardian] :: Nil
  
  MicroService("users_command", actors)

}