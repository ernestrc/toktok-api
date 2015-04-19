package io.toktok.gateway.endpoints

import akka.actor.{ActorSelection, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.novus.salat._
import krakken.model.ctx
import io.toktok.command.users.actors.SessionCommandGuardian
import io.toktok.gateway.ApiConfig
import io.toktok.model.{GenerateTokenCommand, GetUserSession, TokenCreatedEvent, UserSession}
import io.toktok.query.users.actors.SessionQueryGuardian
import krakken.http.Endpoint
import spray.json.DefaultJsonProtocol._
import spray.routing.{PathMatchers, Route}

class SessionEndpoint(implicit val system: ActorSystem) extends Endpoint {

  import krakken.utils.Implicits.pimpedFutureOfReceipt
  import system.dispatcher

  override val fallbackTimeout: Timeout = ApiConfig.ENDPOINT_FALLBACK_TIMEOUT
  override implicit val timeout: Timeout = ApiConfig.ENDPOINT_TIMEOUT


  override val remoteQueryLoc: String = ApiConfig.USERS_QUERY_LOCATION
  override val remoteQueryGuardianPath: String = classOf[SessionQueryGuardian].getSimpleName
  override val remoteCommandLoc: String = ApiConfig.USERS_CMD_LOCATION
  override val remoteCommandGuardianPath: String = classOf[SessionCommandGuardian].getSimpleName

  override val route: (ActorSelection, ActorSelection) ⇒ Route = { (commandGuardian, queryGuardian) ⇒
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
