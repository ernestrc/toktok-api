package io.toktok.query.users.actors

import akka.actor.{Actor, ActorLogging}

/**
 * Created by ernest on 4/14/15.
 */
class UserQueryGuardian extends Actor with ActorLogging {
  override def receive: Actor.Receive = {
    case anyElse ⇒ log.error("Oops!")
  }
}
