package io.toktok.gateway.endpoints

import akka.actor.{ActorSelection, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.novus.salat.global._
import com.novus.salat.grater
import io.toktok.command.users.actors.{ChangeUserPasswordCommand, CreateUserCommand, ForgotPasswordCommand, UsersCommandSideActor}
import io.toktok.gateway.ApiConfig
import krakken.http.Endpoint
import krakken.model.Receipt
import krakken.utils.Implicits._
import spray.routing.Route

//TODO authentication and authorization
//TODO use atom's user guid to help authenticate
//TODO hash passwords
class UserEndpoint(implicit val system: ActorSystem) extends Endpoint {

  import system.dispatcher

  val log = system.log

  val remoteSystemLoc: String = ApiConfig.USERS_CMD_LOCATION
  val remoteGuardianPath = classOf[UsersCommandSideActor].getSimpleName

  val guardianActorSelection: ActorSelection = system.actorSelection(remoteSystemLoc / remoteGuardianPath)
  implicit val receiptGrater = graterMarshallerConverter(Receipt.receiptGrater)
  implicit val graterCreateUser = grater[CreateUserCommand]

  implicit val timeout: Timeout = ApiConfig.ENDPOINT_TIMEOUT
  val fallbackTimeout: Timeout = ApiConfig.ENDPOINT_FALLBACK_TIMEOUT

  override val route: (ActorSelection) ⇒ Route = { guardian ⇒
    pathPrefix("users") {
      pathEndOrSingleSlash {
        post {
          entity(as[CreateUserCommand](graterCreateUser)) { cmd: CreateUserCommand ⇒
            complete {
              guardianActorSelection.ask(cmd).mapTo[Receipt]
            }
          }
        } ~ put {
          complete {
            ???
          }
        }
      } ~ path("password") {
        post {
          entity(as[ChangeUserPasswordCommand](grater[ChangeUserPasswordCommand])) { cmd ⇒
            complete {
              entityActor(cmd.entityId).ask(cmd)(fallbackTimeout)
                .recoverWith {
                case exception: Exception ⇒
                  log.warning(s"Worker of ${cmd.entityId} is not responding!")
                  guardianActorSelection.ask(cmd)
              }.mapTo[Receipt]
            }
          }
        }
      } ~ path("recover") {
        post {
          entity(as[ForgotPasswordCommand](grater[ForgotPasswordCommand])) { cmd ⇒
            complete {
              guardianActorSelection.ask(cmd).mapTo[Receipt]
            }
          }
        }
      }
    }
  }
}
