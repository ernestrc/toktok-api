package io.toktok.api.endpoints

import akka.actor.ActorSystem
import akka.util.Timeout
import com.novus.salat.global.ctx
import com.novus.salat.grater
import io.toktok.config.GlobalConfig
import io.toktok.service.actors.ChangeUserPasswordCommand
import io.toktok.utils.Implicits._
import spray.routing.Route

/**
 * Created by ernest on 4/4/15.
 */
class InternalEndpoint(implicit val system: ActorSystem) extends Endpoint {

  val entityPath: String = "internal"

  implicit val timeout: Timeout = GlobalConfig.ENDPOINT_TIMEOUT
  val fallbackTimeout: Timeout = GlobalConfig.ENDPOINT_FALLBACK_TIMEOUT

  implicit val cmdGrater = graterMarshallerConverter(grater[ChangeUserPasswordCommand])

  override def route: Route = path("internal"){
    post{
      complete{
        ???
      }
    } ~ get{
//      parameters('username.as[String]) { username ⇒
        complete{
          ChangeUserPasswordCommand("551e0d62d4c615edb9da7dc5","heai", "haio")
        }
//      }
    } ~ put{
      complete{

        ???
      }
    }
  }

  system.log.debug(s"Endpoint$$$clazz booted up with timeout $timeout and actor-fallback timeout $fallbackTimeout")

}
