import akka.actor.Props
import akka.event.LoggingAdapter
import akka.io.IO
import api.HttpHandler
import api.endpoints.UserEndpoint
import config.GlobalConfig
import spray.can.Http
import system.BootedSystem
import system.actors.UserActor

object Boot extends App with BootedSystem {

  implicit val log: LoggingAdapter = system.log

  val userActor = system.actorOf(Props[UserActor], "user")

  val endpoints = new UserEndpoint() :: Nil

  val httpHandler = system.actorOf(Props(classOf[HttpHandler], endpoints))

  IO(Http) ! Http.Bind(httpHandler, GlobalConfig.HOST, port = GlobalConfig.PORT)

  log.info(s"TokTok api booted up in host ${GlobalConfig.HOST} and port ${GlobalConfig.PORT}")

  }
