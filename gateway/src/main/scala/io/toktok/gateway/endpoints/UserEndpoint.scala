package io.toktok.gateway.endpoints

import akka.actor.{ActorContext, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.novus.salat.grater
import io.toktok.command.users.actors.UserCommandGuardian
import io.toktok.gateway.ApiConfig
import io.toktok.model._
import io.toktok.query.users.actors.UserQueryGuardian
import krakken.http.GatewayEndpoint
import krakken.model.ctx
import krakken.utils.Implicits.{graterFromResponseUnmarshaller, pimpedFutureOfReceipt}
import spray.routing.Route

class UserEndpoint(implicit val context: ActorContext) extends GatewayEndpoint {

  import context.dispatcher

  val commandService: String = "users_command"
  val queryService: String = "users_query"
  val remoteCommandGuardianPath = classOf[UserCommandGuardian].getSimpleName
  val remoteQueryGuardianPath: String = classOf[UserQueryGuardian].getSimpleName

  implicit val timeout: Timeout = ApiConfig.ENDPOINT_TIMEOUT
  val fallbackTimeout: Timeout = ApiConfig.ENDPOINT_FALLBACK_TIMEOUT

  implicit val graterCreateUser = grater[CreateUserCommand]

  override val route: (ActorRef, ActorRef) ⇒ Route = { (commandGuardian, queryGuardian) ⇒
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
