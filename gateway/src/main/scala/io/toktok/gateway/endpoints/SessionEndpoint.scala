package io.toktok.gateway.endpoints

import akka.actor.{ActorSelection, ActorSystem}
import akka.util.Timeout
import com.novus.salat._
import com.novus.salat.global.ctx
import io.toktok.command.users.actors.SessionCommandGuardian
import io.toktok.gateway.ApiConfig
import krakken.http.Endpoint
import krakken.utils.Implicits._
import spray.routing.Route

class SessionEndpoint(implicit val system: ActorSystem) extends Endpoint {

  override val fallbackTimeout: Timeout = ApiConfig.ENDPOINT_FALLBACK_TIMEOUT
  override implicit val timeout: Timeout = ApiConfig.ENDPOINT_TIMEOUT


  override val remoteQueryLoc: String = ApiConfig.USERS_QUERY_LOCATION
  override val remoteQueryGuardianPath: String = ""//classOf[SessionQueryGuardian].getSimpleName
  override val remoteCommandLoc: String = ApiConfig.USERS_CMD_LOCATION
  override val remoteCommandGuardianPath: String = classOf[SessionCommandGuardian].getSimpleName

//  implicit val tokenGrater = graterMarshallerConverter(grater[GeneratedToken])

  override val route: (ActorSelection) ⇒ Route = { guardian ⇒
    path("token") {
      get {
        parameters('sessionId.as[String]) { sessionId ⇒
          complete {
            ???
            //TODO receipt should take polimorphic types
            //TODO all commands should reply with Receipt[T]
//            entityCommandActor(sessionId).ask(GenerateTokenCommand(sessionId))
//              .mapTo[Receipt].flatMap {
//              case receipt if receipt.success ⇒
//                queryGuardianActorSelection.ask(GetSessionToken(sessionId)).mapTo[GeneratedToken]
//            }
          }
        }
      }
    }

  }
}
