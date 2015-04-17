package io.toktok.command.users.actors

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor._
import akka.event.LoggingAdapter
import akka.pattern._
import akka.util.Timeout
import com.mongodb.casbah.MongoClient
import com.novus.salat.global.ctx
import com.novus.salat.{Grater, _}
import com.opentok.{MediaMode, OpenTok, SessionProperties}
import io.toktok.command.users.ServiceConfig
import io.toktok.model._
import krakken.config.GlobalConfig
import krakken.dal.MongoSource
import krakken.model._
import krakken.system.EventSourcedCommandActor

import scala.concurrent.Future


class SessionCommandGuardian extends EventSourcedCommandActor[SessionEvent] {

  def newChild(anchor: SessionCreatedAnchor) = {
    context.actorOf(Props(classOf[SessionCommandActor], anchor, source, opentok), anchor.opentokSessionId)
  }

  override def preStart(): Unit = {
    log.info(s"SessionGuardian is up and running in path ${self.path}")
    source.findAllEventsOfType[SessionCreatedAnchor].foreach { anchor ⇒
      log.info(s"Booting up session actor for user ${anchor.userId}")
      newChild(anchor)
    }
  }

  implicit val timeout: Timeout = ServiceConfig.ACTOR_TIMEOUT
  override implicit val entityId: Option[SID] = None
  val opentok = new OpenTok(ServiceConfig.OPENTOK_KEY, ServiceConfig.OPENTOK_SECRET)
  val source: MongoSource[SessionEvent] =
    new MongoSource[SessionEvent](db)

  override val eventProcessor: PartialFunction[Event, Unit] = {
    case anchor: SessionCreatedAnchor ⇒ newChild(anchor)
  }

  override val commandProcessor: PartialFunction[Command, List[SessionEvent]] = {
    case GenerateSessionCommand(id) ⇒
      val properties = new SessionProperties.Builder().mediaMode(MediaMode.ROUTED).build()
      val ses = opentok.createSession(properties)
      SessionCreatedAnchor(id, ses.getSessionId) :: Nil
  }

  val subscriptions: List[Subscription] =
    AkkaSubscription[UserActivatedEvent, GenerateSessionCommand](
      grater[UserActivatedEvent], db, GlobalConfig.collectionsHost(classOf[UserEvent].getSimpleName),
      GlobalConfig.collectionsDB(classOf[UserEvent].getSimpleName)) {
      a ⇒ GenerateSessionCommand(a.entityId)
    } :: Nil

}