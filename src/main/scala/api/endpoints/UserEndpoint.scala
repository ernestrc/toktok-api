package api.endpoints

import akka.actor.{ActorSelection, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import config.GlobalConfig
import model.User
import spray.routing.Route
import system.actors.UserActor

class UserEndpoint(implicit system: ActorSystem) extends Endpoint {
  import system.dispatcher
  import utils.Implicits._
  import model.User.userGrater

  val userActor: ActorSelection = system.actorSelection(system / "user")
  implicit val userMarshaller = graterMarshallerConverter(userGrater)

  implicit val timeout: Timeout = GlobalConfig.ENDPOINT_TIMEOUT

  def route: Route =
    path("users") {
      post {
        entity(as[User](userGrater)){ user: User ⇒
          complete{
            userActor.ask(UserActor.CreateUser(user)).mapTo[User]
          }
        }
      } ~ get {
        complete {
          "world"
        }
      } ~ put {
        complete {
          "!!"
        }
      }
    }

}
