package io.toktok.gateway.endpoints

import akka.actor.{ActorSelection, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import io.toktok.command.users.actors.SessionCommandGuardian
import io.toktok.gateway.ApiConfig
import io.toktok.model.{UserSession, GetUserSession, GenerateTokenCommand, TokenCreatedEvent}
import io.toktok.query.users.actors.SessionQueryGuardian
import krakken.http.Endpoint
import krakken.model.Receipt
import spray.json.DefaultJsonProtocol._
import spray.routing.{PathMatchers, Route}

class SessionEndpoint(implicit val system: ActorSystem) extends Endpoint {

  import system.dispatcher

  override val fallbackTimeout: Timeout = ApiConfig.ENDPOINT_FALLBACK_TIMEOUT
  override implicit val timeout: Timeout = ApiConfig.ENDPOINT_TIMEOUT


  override val remoteQueryLoc: String = ApiConfig.USERS_QUERY_LOCATION
  override val remoteQueryGuardianPath: String = classOf[SessionQueryGuardian].getSimpleName
  override val remoteCommandLoc: String = ApiConfig.USERS_CMD_LOCATION
  override val remoteCommandGuardianPath: String = classOf[SessionCommandGuardian].getSimpleName

  implicit val receiptTokenMarshaller = Receipt.receiptFormat(jsonFormat2(TokenCreatedEvent.apply))
  implicit val receiptSessionMarshaller = Receipt.receiptFormat(jsonFormat2(UserSession.apply))

  override val route: (ActorSelection, ActorSelection) ⇒ Route = { (commandGuardian, queryGuardian) ⇒
    path("token") {
      get {
        parameters('sessionId.as[String]) { sessionId ⇒
          complete {
            entityCommandActor(sessionId).ask(GenerateTokenCommand(sessionId))
              .mapTo[Receipt[TokenCreatedEvent]]
          }
        }
      }
    } ~ path("session" / PathMatchers.Segment) { userId ⇒
      get {
        complete {
          queryGuardian.ask(GetUserSession(userId))
            .mapTo[Receipt[UserSession]]
        }
      }
    }

  }
}
