package system.actors

import akka.actor.Actor
import model.User
import org.joda.time.DateTime
import system.actors.UserActor.CreateUser

object UserActor{
  case class CreateUser(user: User)
}
class UserActor extends Actor{
  override def receive: Receive = {
    case CreateUser(user) ⇒ sender ! user.copy(id = Some(1), created = Some(new DateTime()))
  }
}
