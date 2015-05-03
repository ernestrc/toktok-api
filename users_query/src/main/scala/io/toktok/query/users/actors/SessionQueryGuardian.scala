package io.toktok.query.users.actors

import akka.actor.{ActorRef, Props}
import com.novus.salat._
import io.toktok.model._
import io.toktok.query.users.ServiceConfig
import krakken.dal.{AkkaSubscription, Subscription}
import krakken.model.Receipt.Empty
import krakken.model.{ctx, _}
import krakken.system.EventSourcedQueryActor


class SessionQueryGuardian extends EventSourcedQueryActor[SessionEvent] {

  def newChild(anchor: SessionCreatedAnchor): ActorRef = {
    context.actorOf(Props(classOf[SessionQueryActor], anchor), anchor.userId)
  }

  override implicit val entityId: Option[SID] = None
  override val subscriptionSerializers = sessionEventSerializers
  val subscriptions: List[Subscription] =
    AkkaSubscription.forView[SessionCreatedAnchor](grater[SessionCreatedAnchor],
      db, ServiceConfig.collectionsHost(classOf[SessionEvent].getSimpleName),
      ServiceConfig.collectionsDB(classOf[SessionEvent].getSimpleName)) :: Nil

  override val eventProcessor: PartialFunction[Event, Unit] = {
    case anchor: SessionCreatedAnchor ⇒ newChild(anchor)
  }

  override val queryProcessor: PartialFunction[Query, View] = PartialFunction.empty[Query, View]

  override def receive: Receive = {
    super.receive.orElse {
      case cmd@GetUserSession(userId) ⇒
        context.child(userId).map(_.forward(cmd)).getOrElse {
          log.warning(s"Could not find child session actor for user $userId. Instantiating one now")
          //safe bacause we're only subscribed to SessionCreatedAnchor
          $source.map(_.asInstanceOf[SessionCreatedAnchor]).find(_.userId == userId).map {
            anchor ⇒ newChild(anchor).forward(cmd)
          }.getOrElse(sender() ! Receipt(success = false, Empty(),
            "Session not created probably because user has not been activated yet"))
        }
    }
  }
}
