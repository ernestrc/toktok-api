package io.toktok.notifications

import akka.actor.{ActorSelection, ActorSystem}
import akka.event.LoggingAdapter
import akka.util.Timeout
import krakken.model.ctx
import com.novus.salat.grater
import io.toktok.model.ChangeUserPasswordCommand
import krakken.http.Endpoint
import krakken.utils.Implicits._
import spray.httpx.PlayTwirlSupport
import spray.routing.Route

/**
 * Created by ernest on 4/4/15.
 */
class InternalEndpoint(implicit val system: ActorSystem) extends Endpoint with PlayTwirlSupport {

  override val remoteQueryLoc: String = ""
  override val remoteQueryGuardianPath: String = ""
  override val log: LoggingAdapter = system.log
  override val commandGuardianActorSelection: ActorSelection = system.actorSelection("")
  override val remoteCommandGuardianPath: String = ""
  override val remoteCommandLoc: String = ""
  implicit val timeout: Timeout = Timeout(10L)
  val fallbackTimeout: Timeout = Timeout(20L)

  implicit val cmdGrater = graterMarshallerConverter(grater[ChangeUserPasswordCommand])

  override val route: (ActorSelection, ActorSelection) ⇒ Route = { (commandGuardian, queryGuardian) ⇒
    path("internal") {
      post {
        complete {
          ???
        }
      } ~ get {
        //      parameters('username.as[String]) { username ⇒
        complete {
          html.activate.apply("2213", "ernestrc")
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
