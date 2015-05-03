package io.toktok.query.users.actors

import com.mongodb.casbah._
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import io.toktok.model._
import io.toktok.query.users.ServiceConfig
import krakken.dal.{AkkaSubscription, Subscription}
import krakken.model.{ctx, _}
import krakken.system.EventSourcedQueryActor
import krakken.io._
import org.bson.types.ObjectId

import scala.collection.mutable.ArrayBuffer

class UserQueryGuardian extends EventSourcedQueryActor[UserEvent] {

  import context.dispatcher

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
      UsersList(allByUsername.filter(_.startsWith(query)).toList.foldLeft(List.empty[User]){
        (acc, u) ⇒
          val id = subscriptionsColl.find(MongoDBObject("username" → u)).one().get("_id")
            .asInstanceOf[ObjectId].toString
          User(id, u) :: acc
      })

    case GetOnlineUsersQuery(userId, users) ⇒
      online ++= userId :: Nil
      scheduleRemove(userId)
      UsernamesList(users.foldLeft(List.empty[SID]) {
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
