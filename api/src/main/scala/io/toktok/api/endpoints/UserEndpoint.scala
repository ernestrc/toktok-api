package io.toktok.api.endpoints

import akka.actor.{ActorSelection, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.novus.salat.global._
import com.novus.salat.grater
import io.toktok.config.GlobalConfig
import io.toktok.model.Receipt
import io.toktok.service.actors.{ChangeUserPasswordCommand, CreateUserCommand, ForgotPasswordCommand}
import spray.routing.Route

//TODO authentication and authorization
//TODO use atom's user guid to help authenticate
//TODO hash passwords
class UserEndpoint(implicit val system: ActorSystem) extends Endpoint {

  import io.toktok.utils.Implicits._
  import system.dispatcher

  val entityPath = "users"
  val log = system.log

  val usersSelection: ActorSelection = system.actorSelection(system / entityPath)
  implicit val receiptGrater = graterMarshallerConverter(Receipt.receiptGrater)
  implicit val graterCreateUser = grater[CreateUserCommand]

  implicit val timeout: Timeout = GlobalConfig.ENDPOINT_TIMEOUT
  val fallbackTimeout: Timeout = GlobalConfig.ENDPOINT_FALLBACK_TIMEOUT

  def route: Route =
    path(entityPath) {
      post {
        entity(as[CreateUserCommand](graterCreateUser)) { cmd: CreateUserCommand ⇒
          complete {
            usersSelection.ask(cmd).mapTo[Receipt]
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
                usersSelection.ask(cmd)
            }.mapTo[Receipt]
          }
        }
      }
    } ~ path(entityPath / "recover") {
      post {
        entity(as[ForgotPasswordCommand](grater[ForgotPasswordCommand])) { cmd ⇒
          complete {
            usersSelection.ask(cmd).mapTo[Receipt]
          }
        }
      }
    }

}
