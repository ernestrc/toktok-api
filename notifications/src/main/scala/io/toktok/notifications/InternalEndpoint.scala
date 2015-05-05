package io.toktok.notifications

import java.util.concurrent.TimeUnit

import akka.actor.{ActorContext, ActorRef, ActorSelection}
import akka.event.LoggingAdapter
import akka.util.Timeout
import com.novus.salat.grater
import io.toktok.model.ChangeUserPasswordCommand
import krakken.http.GatewayEndpoint
import krakken.model.ctx
import krakken.utils.Implicits._
import spray.httpx.PlayTwirlSupport
import spray.routing.Route

import scala.concurrent.duration.Duration

/**
 * Created by ernest on 4/4/15.
 */
class InternalEndpoint(implicit val context: ActorContext) extends GatewayEndpoint with PlayTwirlSupport {

  override val queryService: String = ""
  override val remoteQueryGuardianPath: String = ""
  override val log: LoggingAdapter = context.system.log
  override val commandGuardianActorSelection: ActorSelection = context.actorSelection("")
  override val remoteCommandGuardianPath: String = ""
  override val commandService: String = ""
  implicit val timeout: Timeout = Duration(10L, TimeUnit.SECONDS)
  val fallbackTimeout: Timeout = Duration(20L, TimeUnit.SECONDS)

  implicit val cmdGrater = graterMarshallerConverter(grater[ChangeUserPasswordCommand])

  override val route: (ActorRef, ActorRef) ⇒ Route = { (commandGuardian, queryGuardian) ⇒
    path("internal") {
      post {
        complete {
          ???
        }
      } ~ get {
        //      parameters('username.as[String]) { username ⇒
        complete {
          html.activate.apply("2213", "ernestrc", "http://localhost:2828/v1/users/activate/")
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
