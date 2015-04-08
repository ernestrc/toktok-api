package io.toktok.command.users.endpoints

import akka.actor.{ActorSelection, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.novus.salat.global._
import com.novus.salat.grater
import io.toktok.command.users.ServiceConfig
import io.toktok.command.users.actors.{UsersCommandSideActor, ChangeUserPasswordCommand, CreateUserCommand, ForgotPasswordCommand}
import io.toktok.http.Endpoint
import io.toktok.model.Receipt
import io.toktok.utils.Implicits._
import spray.routing.Route

//TODO authentication and authorization
//TODO use atom's user guid to help authenticate
//TODO hash passwords
class UserEndpoint(implicit val system: ActorSystem) extends Endpoint {

  import system.dispatcher

  val entityPath = "users"
  val log = system.log

  val guardianActorPath = classOf[UsersCommandSideActor].getSimpleName
  val guardianActorSelection: ActorSelection = system.actorSelection(system / guardianActorPath)
  implicit val receiptGrater = graterMarshallerConverter(Receipt.receiptGrater)
  implicit val graterCreateUser = grater[CreateUserCommand]

  implicit val timeout: Timeout = ServiceConfig.ENDPOINT_TIMEOUT
  val fallbackTimeout: Timeout = ServiceConfig.ENDPOINT_FALLBACK_TIMEOUT

  def route: Route =
    path(entityPath) {
      post {
        entity(as[CreateUserCommand](graterCreateUser)) { cmd: CreateUserCommand ⇒
          complete {
            guardianActorSelection.ask(cmd).mapTo[Receipt]
          }
        }
      } ~ get {
        parameters('username.as[String]) { username ⇒
          complete {
            ???
            //            usersSelection.ask()
            //          entityActor(cmd.entityId).ask(cmd)(EngineConfig.WORKER_TIMEOUT)
            //            .mapTo[Receipt]

          }
        }
      } ~ put {
        complete {
          ???
        }
      }
    } ~ path(entityPath / "password") {
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
    } ~ path(entityPath / "recover") {
      post {
        entity(as[ForgotPasswordCommand](grater[ForgotPasswordCommand])) { cmd ⇒
          complete {
            guardianActorSelection.ask(cmd).mapTo[Receipt]
          }
        }
      }
    }

}
