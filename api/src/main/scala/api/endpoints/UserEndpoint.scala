package api.endpoints

import akka.actor.{ActorSelection, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.novus.salat._
import com.novus.salat.global._
import config.GlobalConfig
import model.Receipt
import service.actors.CreateUserCommand
import spray.routing.Route

class UserEndpoint(implicit val system: ActorSystem) extends Endpoint {

  import system.dispatcher
  import utils.Implicits._

  val entityPath = "users"

  val usersSelection: ActorSelection = system.actorSelection(system / entityPath)
  implicit val receiptGrater = graterMarshallerConverter(Receipt.receiptGrater)

  implicit val timeout: Timeout = GlobalConfig.ENDPOINT_TIMEOUT

  def route: Route =
    path("users") {
      post {
        entity(as[CreateUserCommand](grater[CreateUserCommand])) { cmd: CreateUserCommand ⇒
          complete {
            usersSelection.ask(cmd).mapTo[Receipt]
          }
        }
      } ~ get {
        complete {
//          entityActor(cmd.entityId).ask(cmd)(EngineConfig.WORKER_TIMEOUT)
//            .mapTo[Receipt]
//            .fallbackTo {
//            log.warning(s"Worker of ${cmd.entityId} is not responding!")
//            cmdSideActor.ask(cmd).mapTo[Receipt]
//          }
          ???
        }
      } ~ put {
        complete {
          ???
        }
      }
    }

}
