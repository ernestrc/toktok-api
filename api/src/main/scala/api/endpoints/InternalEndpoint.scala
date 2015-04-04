package api.endpoints

import akka.actor.ActorSystem
import akka.util.Timeout
import com.novus.salat.global.ctx
import com.novus.salat.grater
import config.GlobalConfig
import service.actors.ChangeUserPasswordCommand
import spray.routing.Route
import utils.Implicits._

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
