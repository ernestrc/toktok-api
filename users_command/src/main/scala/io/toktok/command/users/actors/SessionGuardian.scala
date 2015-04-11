package io.toktok.command.users.actors

import akka.actor.Actor.Receive
import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor._
import akka.event.LoggingAdapter
import akka.util.Timeout
import com.mongodb.casbah.MongoClient
import com.novus.salat.annotations._
import com.novus.salat.global.ctx
import com.novus.salat.{Grater, _}
import com.opentok.{MediaMode, OpenTok, SessionProperties}
import io.toktok.command.users.ServiceConfig
import krakken.config.GlobalConfig
import krakken.dal.MongoSource
import krakken.macros.Macros._
import krakken.model._
import krakken.service.EventSourcedActor

import scala.concurrent.Future


@Salat
sealed trait SessionEvent extends Event
case class SessionCreatedAnchor(userId: SID, opentokSessionId: SID) extends SessionEvent
case class TokenCreatedEvent(userId:SID, sessionId:String, token: String) extends SessionEvent

@Salat
sealed trait SessionCommand extends Command
case class GenerateSessionCommand(userId: SID) extends SessionCommand
case class GenerateTokenCommand(userId: SID) extends SessionCommand

object SessionGuardian {
  val eventSerializers: PartialFunction[TypeHint, Grater[_ <: SessionEvent]] = grateSealed[SessionEvent]
}

class SessionGuardian extends Actor with ActorLogging {

  import context.dispatcher

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(loggingEnabled = true) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Restart
    case _: DeathPactException => Restart
    case _: Exception => Restart
  }

  override def preStart(): Unit = {
    log.info(s"SessionGuardian is up and running in path ${self.path}")
    source.findAllEventsOfType[SessionCreatedAnchor].foreach { anchor ⇒
      log.info(s"Booting up session actor for user ${anchor.userId}")
      context.actorOf(Props(classOf[SessionActor], anchor, source, opentok), anchor.userId)
    }
  }

  implicit val entityId: Option[SID] = None

  implicit val timeout: Timeout = ServiceConfig.ACTOR_TIMEOUT
  implicit val logger: LoggingAdapter = log
  val client = MongoClient(ServiceConfig.mongoHost)
  val db = client(ServiceConfig.mongoDb)
  val opentok = new OpenTok(ServiceConfig.OPENTOK_KEY, ServiceConfig.OPENTOK_SECRET)

  val serializers: PartialFunction[TypeHint, Grater[_ <: SessionEvent]] =
    SessionGuardian.eventSerializers

  val subscriptions: List[Subscription] =
    AkkaSubscription[UserActivatedEvent, GenerateSessionCommand](
      grater[UserActivatedEvent], db, GlobalConfig.collectionsHost("UserEvent")){
      a ⇒ GenerateSessionCommand(a.entityId)
    } :: Nil

  val source: MongoSource[SessionEvent] =
    new MongoSource[SessionEvent](db, serializers)
  
  def generateSessionCommand(userId: SID) = Future {
    val ses = opentok.createSession(
      new SessionProperties.Builder().mediaMode(MediaMode.ROUTED).build())
    val anchor = SessionCreatedAnchor(userId, ses.getSessionId)
    source.save(anchor).get
    anchor
  }.map{ saved ⇒
    val msg = s"Successfully created session for user $userId"
    context.actorOf(Props(classOf[SessionActor], saved, source, opentok), userId)
    log.info(msg)
    Receipt(success = true, updated = saved.opentokSessionId, message = msg)
  }.recover{
    case err: Exception ⇒
      val msg = s"Could not session for user $userId"
      log.error(err, msg)
      Receipt.error(err, msg)
  }

  override def receive: Receive = {
    case GenerateSessionCommand(id) ⇒ generateSessionCommand(id)
  }

}