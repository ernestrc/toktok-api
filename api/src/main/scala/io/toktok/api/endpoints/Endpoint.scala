package io.toktok.api.endpoints

import akka.actor.{ActorSelection, ActorSystem}
import akka.util.Timeout
import io.toktok.api.AuthenticationDirectives
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
