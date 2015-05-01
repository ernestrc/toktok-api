package io.toktok.command.users.actors

import akka.actor._
import akka.util.Timeout
import com.mongodb.casbah.MongoClient
import com.novus.salat._
import com.opentok.{MediaMode, OpenTok, SessionProperties}
import io.toktok.command.users.ServiceConfig
import io.toktok.model._
import krakken.dal.MongoSource
import krakken.model.{ctx, _}
import krakken.system.EventSourcedCommandActor
import krakken.utils.io._


class SessionCommandGuardian extends EventSourcedCommandActor[SessionEvent] {

  def newChild(anchor: SessionCreatedAnchor) = {
    context.actorOf(Props(classOf[SessionCommandActor], anchor, opentok), anchor.opentokSessionId)
  }

  override def preStart(): Unit = {
    log.info(s"SessionGuardian is up and running in path ${self.path}")
    source.findAllEventsOfType[SessionCreatedAnchor].foreach { anchor ⇒
      log.info(s"Booting up session actor for user ${anchor.userId}")
      newChild(anchor)
    }
    subscriptions.foreach(_.subscribe())
  }

  implicit val timeout: Timeout = ServiceConfig.ACTOR_TIMEOUT
  override implicit val entityId: Option[SID] = None
  val opentok = new OpenTok(ServiceConfig.OPENTOK_KEY, ServiceConfig.OPENTOK_SECRET)

  override val eventProcessor: PartialFunction[Event, Unit] = {
    case anchor: SessionCreatedAnchor ⇒ newChild(anchor)
  }

  override val commandProcessor: PartialFunction[Command, List[SessionEvent]] = {
    case GenerateSessionCommand(id) ⇒
      val properties = new SessionProperties.Builder().mediaMode(MediaMode.ROUTED).build()
      val ses = opentok.createSession(properties)
      SessionCreatedAnchor(id, ses.getSessionId) :: Nil
  }

  lazy val subscriptions: List[Subscription] =
    AkkaSubscription[UserActivatedEvent, GenerateSessionCommand](
      grater[UserActivatedEvent], db, ServiceConfig.collectionsHost(classOf[UserEvent].getSimpleName),
      ServiceConfig.collectionsDB(classOf[UserEvent].getSimpleName)) {
      a ⇒ GenerateSessionCommand(a.entityId)
    } :: Nil

}