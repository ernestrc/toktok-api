import akka.actor.Props
import akka.event.LoggingAdapter
import akka.io.IO
import api.HttpHandler
import api.endpoints.{InternalEndpoint, UserEndpoint}
import config.GlobalConfig
import service.BootedSystem
import service.actors.{UserActor, UsersGuardian}
import spray.can.Http

object Boot extends App with BootedSystem {

  implicit val log: LoggingAdapter = system.log

  val users = system.actorOf(Props[UsersGuardian], "users")

  val endpoints = new UserEndpoint() :: new InternalEndpoint() :: Nil

  val httpHandler = system.actorOf(Props(classOf[HttpHandler], endpoints))

  IO(Http) ! Http.Bind(httpHandler, GlobalConfig.HOST, port = GlobalConfig.PORT)

  log.info(s"TokTok api booted up in host ${GlobalConfig.HOST} and port ${GlobalConfig.PORT}")

}
