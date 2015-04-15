package io.toktok.query.users.actors

import akka.actor.{Actor, ActorLogging}
import io.toktok.model.{OnlineUsers, GetOnlineUsersQuery}
import io.toktok.query.users.ServiceConfig
import krakken.model.{Receipt, SID}

import scala.collection.mutable.ArrayBuffer

class UserQueryGuardian extends Actor with ActorLogging {

  import context.dispatcher

  case class OnlineTimeout(userId: SID)

  val online = ArrayBuffer.empty[SID]

  def scheduleRemove(userId: SID) =
    context.system.scheduler.scheduleOnce(
      ServiceConfig.ONLINE_THRESHOLD, self, OnlineTimeout(userId))

  override def receive: Actor.Receive = {
    case OnlineTimeout(userId) ⇒ online.remove(online.indexOf(userId))
    case GetOnlineUsersQuery(userId, users) ⇒
      online ++= userId :: Nil
      scheduleRemove(userId)
      sender() ! Receipt(success = true, entity = Some(OnlineUsers(users.foldLeft(List.empty[SID]) {
        case (acc, user) if online.contains(user) ⇒ user :: acc
        case (acc, user) ⇒ acc
      })))
  }

}
