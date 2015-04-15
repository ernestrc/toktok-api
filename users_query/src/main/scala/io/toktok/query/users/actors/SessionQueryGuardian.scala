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
    context.actorOf(Props(classOf[SessionQueryActor], anchor, source), anchor.userId)
  }

  override def preStart(): Unit = {
    log.info(s"SessionGuardian is up and running in path ${self.path}")
    source.findAllEventsOfType[SessionCreatedAnchor].foreach { anchor ⇒
      log.info(s"Booting up session actor for user ${anchor.userId}")
      newChild(anchor)
    }
  }

  override implicit val entityId: Option[SID] = None
  val client = MongoClient(ServiceConfig.mongoHost)
  val db = client(ServiceConfig.mongoDb)
  val serializers: PartialFunction[TypeHint, Grater[_ <: SessionEvent]] = sessionEventSerializers
  val source: MongoSource[SessionEvent] =
    new MongoSource[SessionEvent](db, serializers)

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
      case cmd @ GetUserSession(userId) ⇒
        context.child(userId).map(_.forward(cmd)).getOrElse{
          log.warning(s"Could not find child session actor for user $userId. Instantiating one now")
          newChild(source.findAllEventsOfType[SessionCreatedAnchor].filter(_.userId == userId).head)
            .forward(cmd)
        }
    }
  }
}
