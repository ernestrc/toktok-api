package io.toktok.query.users.actors

import akka.pattern.ask
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import io.toktok.model._
import io.toktok.query.users.ServiceConfig
import krakken.dal.{AkkaSubscription, Subscription}
import krakken.io._
import krakken.model.{ctx, _}
import krakken.system.EventSourcedQueryActor
import org.bson.types.ObjectId

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.util.Try

class UserQueryGuardian extends EventSourcedQueryActor[UserEvent] {

  val userEvent = classOf[UserEvent].getSimpleName

  import context.dispatcher

  override def preStart(): Unit = {
    val mongoContainer: Option[Service] = Try(Await.result(discoveryActor.ask(
      DiscoveryActor.Find(ServiceConfig.collectionsSourceContainer(userEvent)))(ServiceConfig.ACTOR_TIMEOUT)
      .mapTo[Service], ServiceConfig.ACTOR_TIMEOUT))
      .toOption
    val mongoHost: String =  mongoContainer.map(_.host.ip).getOrElse(ServiceConfig.collectionsConfigHost(userEvent))
    val mongoPort: Int = mongoContainer.map(_.port).getOrElse(ServiceConfig.collectionsPort(userEvent))
    userEventSourceHost = Some(mongoHost)
    userEventSourcePort = Some(mongoPort)
    super.preStart()
  }

  case class OnlineTimeout(userId: SID)

  val allByUsername = ArrayBuffer.empty[SID]
  val online = ArrayBuffer.empty[SID]

  var userEventSourceHost: Option[String] = None
  var userEventSourcePort: Option[Int] = None

  override implicit val entityId: Option[SID] = None

  override val subscriptionSerializers: FromHintGrater[AnyRef] =
    userEventSerializers

  override lazy val subscriptions: List[Subscription] =
    AkkaSubscription.forView[UserCreatedAnchor](grater[UserCreatedAnchor],
      db, userEventSourceHost.get, userEventSourcePort.get,
      ServiceConfig.collectionsDB(userEvent)) :: Nil

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
