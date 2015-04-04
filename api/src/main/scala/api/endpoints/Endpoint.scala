package api.endpoints

import akka.actor.{ActorSystem, ActorSelection}
import akka.util.Timeout
import api.AuthenticationDirectives
import com.novus.salat.Context
import config.GlobalConfig
import model.SID
import org.bson.types.ObjectId
import spray.httpx.SprayJsonSupport
import spray.routing._

/**
 * Http endpoint interface
 */
trait Endpoint extends Directives with SprayJsonSupport with AuthenticationDirectives {

  val system: ActorSystem
  val entityPath: String
  val clazz = this.getClass.getCanonicalName

  implicit val timeout: Timeout
  val fallbackTimeout: Timeout

  def route: Route

  def entityActor(entityId: String): ActorSelection =
    system.actorSelection(system / entityPath / entityId.toString)

}
