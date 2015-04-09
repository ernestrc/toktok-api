package io.toktok.command.users

import akka.actor.Props
import io.toktok.command.users.actors.{SessionActor, UsersCommandSideActor}
import io.toktok.model.MicroService

object Boot extends App {

  val actors = Props[UsersCommandSideActor] :: Props[SessionActor] :: Nil
  
  MicroService("users_command", actors)

}