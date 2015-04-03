package api.endpoints

import akka.actor.{ActorSystem, ActorSelection}
import akka.util.Timeout
import api.AuthenticationDirectives
import com.novus.salat.Context
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

  implicit val timeout: Timeout

  def route: Route

  def entityActor(entityId: ObjectId): ActorSelection =
    system.actorSelection(system / entityPath / entityId.toString)

}
