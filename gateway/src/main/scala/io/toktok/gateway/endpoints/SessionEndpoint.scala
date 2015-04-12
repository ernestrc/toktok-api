package io.toktok.gateway.endpoints

import akka.actor.{ActorSelection, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.novus.salat._
import com.novus.salat.global.ctx
import io.toktok.gateway.ApiConfig
import io.toktok.model.{GenerateTokenCommand, GeneratedToken, GetSessionToken}
import krakken.http.Endpoint
import krakken.model.Receipt
import krakken.utils.Implicits._
import spray.routing.Route

class SessionEndpoint(implicit val system: ActorSystem) extends Endpoint {

  import system.dispatcher

  override val fallbackTimeout: Timeout = ApiConfig.ENDPOINT_FALLBACK_TIMEOUT
  override implicit val timeout: Timeout = ApiConfig.ENDPOINT_TIMEOUT


  override val remoteQueryLoc: String = ApiConfig.USERS_QUERY_LOCATION
  override val remoteQueryGuardianPath: String = "SessionActor"
  override val remoteCommandLoc: String = ApiConfig.USERS_CMD_LOCATION
  override val remoteCommandGuardianPath: String = "SessionGuardian"

  implicit val tokenGrater = graterMarshallerConverter(grater[GeneratedToken])

  override val route: (ActorSelection) ⇒ Route = { guardian ⇒
    path("token") {
      get {
        parameters('userId.as[String]) { userId ⇒
          complete {
            val cmd = GenerateTokenCommand(userId)
            entityCommandActor(userId).ask(cmd)
              .mapTo[Receipt].flatMap {
              case receipt if receipt.success ⇒
                queryGuardianActorSelection.ask(GetSessionToken(userId)).mapTo[GeneratedToken]
            }
          }
        }
      }
    }

  }
}
