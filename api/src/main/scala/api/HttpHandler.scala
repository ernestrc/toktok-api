package api

import akka.actor.{Actor, ActorRefFactory}
import api.endpoints.Endpoint
import spray.routing.{Route, HttpService}

class HttpHandler(endpoints: List[Endpoint]) extends HttpService with Actor with DefaultHttpConfig {

  override implicit def actorRefFactory: ActorRefFactory = context.system

  val routes: Route = {
    var _routes: Route = null
    endpoints.foreach { endpoint =>
      val route = endpoint.route
      _routes = {
        if (_routes != null) _routes ~ route
        else route
      }
    }
    _routes
  }

  def receive: Receive =
    runRoute(routes)(exceptionHandler, rejectionHandler,
      context, routingSettings, loggingContext)
}
