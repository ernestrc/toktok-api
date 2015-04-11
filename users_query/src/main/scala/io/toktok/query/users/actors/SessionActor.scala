package io.toktok.query.users.actors

import akka.actor.{Actor, ActorLogging}
import com.mongodb.casbah.MongoClient
import com.novus.salat._
import io.toktok.command.users.actors._
import io.toktok.query.users.ServiceConfig
import krakken.config.GlobalConfig
import krakken.dal.MongoSource
import krakken.model.{AkkaSubscription, SID, Subscription, TypeHint}


//TODO refactor messages out to model project
//TODO think about how to make the persistance of subscriptions seamslessly in views as well
//TODO design QueryActor[T]
case class GeneratedToken(token: String, userId: SID, sessionId: SID)
case class GetSessionToken(userId: SID)

class SessionActor extends Actor with ActorLogging {

  val client = MongoClient(ServiceConfig.mongoHost)
  val db = client(ServiceConfig.mongoDb)

  val serializers: PartialFunction[TypeHint, Grater[_ <: SessionEvent]] =
    SessionGuardian.eventSerializers

  val subscriptions: List[Subscription] =
    AkkaSubscription[TokenCreatedEvent, GenerateSessionCommand](
      grater[UserActivatedEvent], db, GlobalConfig.collectionsHost("UserEvent")){
      a ⇒ GenerateSessionCommand(a.entityId)
    } :: Nil

  val source: MongoSource[SessionEvent] =
    new MongoSource[SessionEvent](db, serializers)

  override def receive: Receive = {
    case GeneratedToken ⇒ log.error("OOPS!!!!!!")
  }
}
