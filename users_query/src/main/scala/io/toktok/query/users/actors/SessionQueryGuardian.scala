package io.toktok.query.users.actors

import akka.actor.{ActorRef, Props}
import com.mongodb.casbah.MongoClient
import com.novus.salat._
import io.toktok.model._
import io.toktok.query.users.ServiceConfig
import krakken.config.GlobalConfig
import krakken.dal.MongoSource
import krakken.model.{TypeHint, _}
import krakken.system.EventSourcedQueryActor
import com.novus.salat.global.ctx


class SessionQueryGuardian extends EventSourcedQueryActor[SessionEvent] {

  def newChild(anchor:SessionCreatedAnchor):ActorRef = {
    context.actorOf(Props(classOf[SessionQueryActor], anchor), anchor.userId)
  }

  override implicit val entityId: Option[SID] = None
  override val subscriptionSerializers = sessionEventSerializers
  val subscriptions: List[Subscription] =
    AkkaSubscription.forView[SessionCreatedAnchor](grater[SessionCreatedAnchor],
      db, GlobalConfig.collectionsHost(classOf[SessionEvent].getSimpleName),
      GlobalConfig.collectionsDB(classOf[SessionEvent].getSimpleName)) :: Nil

  override val eventProcessor: PartialFunction[Event, Unit] = {
    case anchor: SessionCreatedAnchor ⇒ newChild(anchor)
  }

  override val queryProcessor: PartialFunction[Query, View] = PartialFunction.empty[Query, View]

  override def receive: Receive = {
    super.receive.orElse{
      case cmd @ GetUserSession(userId) ⇒ log.error("OOps! I could not redirect! FIX ME!")
//        context.child(userId).map(_.forward(cmd)).getOrElse{
//          log.warning(s"Could not find child session actor for user $userId. Instantiating one now")
//          newChild($source.filter(_.userId == userId).head)
//            .forward(cmd)
//        }
    }
  }
}
