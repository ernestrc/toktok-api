package io.toktok.command.users.actors

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingAdapter
import akka.util.Timeout
import com.mongodb.casbah.MongoClient
import com.novus.salat.Grater
import com.novus.salat.annotations._
import io.toktok.command.users.ServiceConfig
import io.toktok.config.GlobalConfig
import io.toktok.dal.MongoSource
import io.toktok.model._
import io.toktok.service.EventSourcedActor
import unstable.macros.TypeHint
import com.novus.salat._
import com.novus.salat.global.ctx
import unstable.macros.Macros._
import com.opentok.{MediaMode, SessionProperties, OpenTok}


@Salat
sealed trait SessionEvent extends Event
case class SessionCreatedEvent(uuid: SID, sessionID: SID) extends SessionEvent

@Salat
sealed trait SessionCommand extends Command
case class GenerateSessionCommand(userId: SID) extends SessionCommand

object SessionActor {
  val eventSerializers: PartialFunction[TypeHint, Grater[_ <: SessionEvent]] = grateSealed[SessionEvent]
}

class SessionActor extends EventSourcedActor[SessionEvent] {

  override implicit val entityId: Option[SID] = None

  implicit val timeout: Timeout = ServiceConfig.ACTOR_TIMEOUT
  implicit val logger: LoggingAdapter = log
  val client = MongoClient(ServiceConfig.mongoHost)
  val db = client(ServiceConfig.mongoDb)
  val opentok = new OpenTok(ServiceConfig.OPENTOK_KEY, ServiceConfig.OPENTOK_SECRET)

  val serializers: PartialFunction[TypeHint, Grater[_ <: SessionEvent]] =
    SessionActor.eventSerializers

  val source: MongoSource[SessionEvent] =
    new MongoSource[SessionEvent](db, serializers)


  override val eventProcessor: PartialFunction[Event, Unit] = {
    case cmd @ SessionCreatedEvent(uuid, sid) ⇒
      log.info(s"Created Opentok session for user $uuid")
  }

  override val commandProcessor: PartialFunction[Command, List[SessionEvent]] = {
    case GenerateSessionCommand(id) ⇒
      val ses = opentok.createSession(
        new SessionProperties.Builder().mediaMode(MediaMode.ROUTED).build())
      SessionCreatedEvent(id, ses.getSessionId) :: Nil
  }

  val subscriptions: List[Subscription] =
    AkkaSubscription[UserActivatedEvent, GenerateSessionCommand](
      grater[UserActivatedEvent], db, GlobalConfig.collectionsHost("UserEvent")){
      a ⇒ GenerateSessionCommand(a.entityId)
    } :: Nil
}
