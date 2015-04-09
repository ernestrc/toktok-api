package io.toktok.gateway.endpoints

import akka.actor.{ActorSelection, ActorSystem}
import akka.event.LoggingAdapter
import akka.util.Timeout
import com.novus.salat.global.ctx
import com.novus.salat.grater
import io.toktok.command.users.actors.ChangeUserPasswordCommand
import io.toktok.gateway.ApiConfig
import io.toktok.http.Endpoint
import io.toktok.utils.Implicits._
import spray.routing.Route

/**
 * Created by ernest on 4/4/15.
 */
class InternalEndpoint(implicit val system: ActorSystem) extends Endpoint {

  override val log: LoggingAdapter = system.log
  override val guardianActorSelection: ActorSelection = system.actorSelection("")
  override val remoteGuardianPath: String = ""
  override val remoteSystemLoc: String = ""
  implicit val timeout: Timeout = ApiConfig.ENDPOINT_TIMEOUT
  val fallbackTimeout: Timeout = ApiConfig.ENDPOINT_FALLBACK_TIMEOUT

  implicit val cmdGrater = graterMarshallerConverter(grater[ChangeUserPasswordCommand])

  override val route: (ActorSelection) ⇒ Route = { guardian ⇒
    path("internal") {
      post {
        complete {
          ???
        }
      } ~ get {
        //      parameters('username.as[String]) { username ⇒
        complete {
          ChangeUserPasswordCommand("551e0d62d4c615edb9da7dc5", "heai", "haio")
        }
        //      }
      } ~ put {
        complete {

          ???
        }
      }
    }
  }
}
