package io.toktok.command.users

import akka.actor.Props
import io.toktok.command.users.actors.UsersCommandSideActor
import io.toktok.command.users.endpoints.{InternalEndpoint, UserEndpoint}
import io.toktok.model.{EndpointProps, MicroService}

/**
 * Created by ernest on 4/5/15.
 */
object Boot extends App {

  val endpoints = EndpointProps[UserEndpoint] :: EndpointProps[InternalEndpoint] :: Nil
  val actors = Props[UsersCommandSideActor] :: Nil
  
  MicroService("users_command", ServiceConfig.HOST, ServiceConfig.PORT, actors, endpoints)

}