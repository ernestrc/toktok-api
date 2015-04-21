package io.toktok.query.users.actors

import com.mongodb.casbah.{Imports, MongoClient}
import com.novus.salat._
import io.toktok.model._
import io.toktok.query.users.ServiceConfig
import krakken.model.{ctx, _}
import krakken.system.EventSourcedQueryActor

import scala.collection.mutable.ArrayBuffer

class UserQueryGuardian extends EventSourcedQueryActor[UserEvent] {

  import context.dispatcher

  override val db: Imports.MongoDB =
    MongoClient(ServiceConfig.mongoHost, ServiceConfig.mongoPort)(ServiceConfig.dbName)

  case class OnlineTimeout(userId: SID)

  val allByUsername = ArrayBuffer.empty[SID]
  val online = ArrayBuffer.empty[SID]

  override implicit val entityId: Option[SID] = None

  override val subscriptionSerializers: FromHintGrater[AnyRef] =
    userEventSerializers

  override val subscriptions: List[Subscription] =
    AkkaSubscription.forView[UserCreatedAnchor](grater[UserCreatedAnchor],
      db, ServiceConfig.collectionsHost(classOf[UserEvent].getSimpleName),
      ServiceConfig.collectionsDB(classOf[UserEvent].getSimpleName)) :: Nil
  
  override val queryProcessor: PartialFunction[Query, View] = {
    case GetUsersByUsername(query) ⇒
      UsersList(allByUsername.filter(_.startsWith(query)).toList)
    case GetOnlineUsersQuery(userId, users) ⇒
      online ++= userId :: Nil
      scheduleRemove(userId)
      UsersList(users.foldLeft(List.empty[SID]) {
        case (acc, user) if online.contains(user) ⇒ user :: acc
        case (acc, user) ⇒ acc
      })
  }
  
  override val eventProcessor: PartialFunction[Event, Unit] = {
    case anchor: UserCreatedAnchor ⇒ allByUsername += anchor.username
  }

  def scheduleRemove(userId: SID) =
    context.system.scheduler.scheduleOnce(
      ServiceConfig.ONLINE_THRESHOLD, self, OnlineTimeout(userId))

  override def receive: Receive = super.receive.orElse{
      case OnlineTimeout(userId) ⇒ online.remove(online.indexOf(userId))
    }

}
