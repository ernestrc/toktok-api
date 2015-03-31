package api.endpoints

import akka.util.Timeout
import api.AuthenticationDirectives
import spray.httpx.SprayJsonSupport
import spray.routing._

/**
 * Http endpoint interface
 */
trait Endpoint extends Directives with SprayJsonSupport with AuthenticationDirectives{

  implicit val timeout: Timeout

  def route: Route

}
