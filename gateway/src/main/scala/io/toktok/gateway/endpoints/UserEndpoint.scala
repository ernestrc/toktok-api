package io.toktok.gateway.endpoints

import akka.actor.{ActorSelection, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.novus.salat.global._
import com.novus.salat.grater
import io.toktok.command.users.actors.UserCommandGuardian
import io.toktok.gateway.ApiConfig
import io.toktok.model.{ChangeUserPasswordCommand, CreateUserCommand, ForgotPasswordCommand}
import io.toktok.query.users.actors.UserQueryGuardian
import krakken.http.Endpoint
import krakken.model.{SID, Receipt}
import krakken.utils.Implicits._
import spray.routing.Route

class UserEndpoint(implicit val system: ActorSystem) extends Endpoint {

  import system.dispatcher

  val remoteCommandLoc: String = ApiConfig.USERS_CMD_LOCATION
  val remoteCommandGuardianPath = classOf[UserCommandGuardian].getSimpleName

  override val remoteQueryLoc: String = ApiConfig.USERS_QUERY_LOCATION
  override val remoteQueryGuardianPath: String = classOf[UserQueryGuardian].getSimpleName

  implicit val timeout: Timeout = ApiConfig.ENDPOINT_TIMEOUT
  val fallbackTimeout: Timeout = ApiConfig.ENDPOINT_FALLBACK_TIMEOUT

  implicit val graterCreateUser = grater[CreateUserCommand]

  override val route: (ActorSelection, ActorSelection) ⇒ Route = { (commandGuardian, queryGuardian) ⇒
    pathPrefix("users") {
      pathEndOrSingleSlash {
        post {
          entity(as[CreateUserCommand](graterCreateUser)) { cmd: CreateUserCommand ⇒
            complete {
              commandGuardian.ask(cmd).mapTo[Receipt[SID]]
            }
          }
        }
      } ~ path("password") {
        post {
          entity(as[ChangeUserPasswordCommand](grater[ChangeUserPasswordCommand])) { cmd ⇒
            complete {
              entityCommandActor(cmd.entityId).ask(cmd)(fallbackTimeout)
                .recoverWith {
                case exception: Exception ⇒
                  log.warning(s"Worker of ${cmd.entityId} is not responding!")
                  commandGuardian.ask(cmd)
              }.mapTo[Receipt[Unit]]
            }
          }
        }
      } ~ path("recover") {
        post {
          entity(as[ForgotPasswordCommand](grater[ForgotPasswordCommand])) { cmd ⇒
            complete {
              commandGuardian.ask(cmd).mapTo[Receipt[Unit]]
            }
          }
        }
      }
    }
  }
}
