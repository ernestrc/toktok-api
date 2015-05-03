package io.toktok.gateway.endpoints

import akka.actor.{ActorSelection, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.novus.salat.grater
import io.toktok.command.users.actors.UserCommandGuardian
import io.toktok.gateway.ApiConfig
import io.toktok.model._
import io.toktok.query.users.actors.UserQueryGuardian
import krakken.http.CQRSEndpoint
import krakken.model.ctx
import krakken.utils.Implicits.{graterFromResponseUnmarshaller, pimpedFutureOfReceipt}
import krakken.io._
import spray.routing.Route

class UserEndpoint(implicit val system: ActorSystem) extends CQRSEndpoint {

  import system.dispatcher

  val remoteCommandLoc: String = getContainerLink("users_command")
    .map(_.toAkkaUrl).getOrElse(ApiConfig.USERS_CMD_LOCATION)
  val remoteCommandGuardianPath = classOf[UserCommandGuardian].getSimpleName

  override val remoteQueryLoc: String = getContainerLink("users_query")
    .map(_.toAkkaUrl).getOrElse(ApiConfig.USERS_QUERY_LOCATION)
  override val remoteQueryGuardianPath: String = classOf[UserQueryGuardian].getSimpleName

  implicit val timeout: Timeout = ApiConfig.ENDPOINT_TIMEOUT
  val fallbackTimeout: Timeout = ApiConfig.ENDPOINT_FALLBACK_TIMEOUT

  implicit val graterCreateUser = grater[CreateUserCommand]

  override val route: (ActorSelection, ActorSelection) ⇒ Route = { (commandGuardian, queryGuardian) ⇒
    pathPrefix("v1") {
      pathPrefix("users") {
        pathEndOrSingleSlash {
          post {
            entity(as[CreateUserCommand](graterCreateUser)) { cmd: CreateUserCommand ⇒
              complete {
                commandGuardian.ask(cmd).>>>[String]
              }
            }
          } ~ get {
            parameter('query.as[String]) { query ⇒
              complete {
                queryGuardian.ask(GetUsersByUsername(query)).>>>[UsersList]
              }
            }
          }
        } ~ path("password") {
          post {
            entity(as[ChangeUserPasswordCommand](grater[ChangeUserPasswordCommand])) { cmd ⇒
              complete {
                (entityCommandActor(cmd.entityId) ?? cmd).>>>[PasswordChangedEvent]
              }
            }
          }
        } ~ path("recover") {
          post {
            entity(as[ForgotPasswordCommand](grater[ForgotPasswordCommand])) { cmd ⇒
              complete {
                commandGuardian.ask(cmd).>>>[PasswordChangedEvent]
              }
            }
          }
        } ~ path("activate" / Segment) { userId ⇒
          get {
            complete {
              (entityCommandActor(userId) ?? ActivateUserCommand(userId))
                .>>>[UserActivatedEvent]
            }
          }
        }
      } ~ pathPrefix("online") {
        pathEndOrSingleSlash {
          post {
            entity(as[GetOnlineUsersQuery](grater[GetOnlineUsersQuery])) { cmd ⇒
              complete {
                queryGuardian.ask(cmd).>>>[UsernamesList]
              }
            }
          }
        }
      }
    }
  }
}
