package io.toktok

import akka.actor.Props
import akka.event.LoggingAdapter
import akka.io.IO
import io.toktok.api.HttpHandler
import io.toktok.api.endpoints.{InternalEndpoint, UserEndpoint}
import io.toktok.config.GlobalConfig
import io.toktok.service.BootedSystem
import io.toktok.service.actors.UsersGuardian
import spray.can.Http

/**
 * Created by ernest on 4/5/15.
 */
object Boot extends App with BootedSystem {

  implicit val log: LoggingAdapter = system.log

  val users = system.actorOf(Props[UsersGuardian], "users")

  val endpoints = new UserEndpoint() :: new InternalEndpoint() :: Nil

  val httpHandler = system.actorOf(Props(classOf[HttpHandler], endpoints))

  IO(Http) ! Http.Bind(httpHandler, GlobalConfig.HOST, port = GlobalConfig.PORT)

  log.info(s"TokTok api booted up in host ${GlobalConfig.HOST} and port ${GlobalConfig.PORT}")

}
