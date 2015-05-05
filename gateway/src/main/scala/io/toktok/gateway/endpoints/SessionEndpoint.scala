package io.toktok.gateway.endpoints

import akka.actor.{ActorContext, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import io.toktok.command.users.actors.SessionCommandGuardian
import io.toktok.gateway.ApiConfig
import io.toktok.model.{GenerateTokenCommand, GetUserSession, TokenCreatedEvent, UserSession}
import io.toktok.query.users.actors.SessionQueryGuardian
import krakken.http.GatewayEndpoint
import spray.routing.{PathMatchers, Route}

class SessionEndpoint(val context: ActorContext) extends GatewayEndpoint {

  import context.dispatcher
  import krakken.utils.Implicits.pimpedFutureOfReceipt

  override val fallbackTimeout: Timeout = ApiConfig.ENDPOINT_FALLBACK_TIMEOUT
  override implicit val timeout: Timeout = ApiConfig.ENDPOINT_TIMEOUT

  val commandService: String = "users_command"
  override val queryService: String = "users_query"
  override val remoteQueryGuardianPath: String = classOf[SessionQueryGuardian].getSimpleName
  override val remoteCommandGuardianPath: String = classOf[SessionCommandGuardian].getSimpleName

  override val route: (ActorRef, ActorRef) ⇒ Route = { (commandGuardian, queryGuardian) ⇒
    pathPrefix("v1") {
      path("token") {
        get {
          parameters('sessionId.as[String]) { sessionId ⇒
            complete {
              entityCommandActor(sessionId).ask(GenerateTokenCommand(sessionId)).>>>[TokenCreatedEvent]
            }
          }
        }
      } ~ path("session" / PathMatchers.Segment) { userId ⇒
        get {
          complete {
            queryGuardian.ask(GetUserSession(userId)).>>>[UserSession]
          }
        }
      }
    }
  }
}
