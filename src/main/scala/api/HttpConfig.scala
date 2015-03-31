package api

import akka.actor.{ActorRefFactory, Actor}
import spray.http.StatusCode
import spray.http.StatusCodes._
import spray.routing.{RequestContext, RoutingSettings, RejectionHandler, ExceptionHandler}
import spray.util.LoggingContext

trait HttpConfig{

  val exceptionHandler: ExceptionHandler

  val rejectionHandler: RejectionHandler

  val routingSettings: RoutingSettings

  val loggingContext: LoggingContext

}

trait DefaultHttpConfig extends HttpConfig {

  implicit def actorRefFactory: ActorRefFactory

  private def loggedFailureResponse(ctx: RequestContext,thrown: Throwable,
                                    message: String = "Something Exploded!",
                                    error: StatusCode = InternalServerError): Unit = {
    ctx.complete((error, s"$message CAUSE $thrown"))
  }

  val exceptionHandler: ExceptionHandler = ExceptionHandler {

    case e: IllegalArgumentException => ctx =>
      loggedFailureResponse(ctx, e,
        message = "Illegan argument Exception thrown: " + e.getMessage,
        error = NotAcceptable)

    case e: NoSuchElementException => ctx =>
      loggedFailureResponse(ctx, e,
        message = "Not Found",
        error = NotFound)

    case t: Throwable => ctx =>
      loggedFailureResponse(ctx, t)
  }

  val routingSettings: RoutingSettings = RoutingSettings.default
  val loggingContext: LoggingContext = LoggingContext.fromActorRefFactory
  val rejectionHandler: RejectionHandler = RejectionHandler.Default

}
