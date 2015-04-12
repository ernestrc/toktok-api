package io.toktok.query.users.actors

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingAdapter
import com.mongodb.casbah.MongoClient
import com.novus.salat._
import io.toktok.model._
import io.toktok.query.users.ServiceConfig
import krakken.dal.MongoSource
import krakken.model.TypeHint


class SessionActor extends Actor with ActorLogging {

  override implicit val log: LoggingAdapter = context.system.log

  val client = MongoClient(ServiceConfig.mongoHost)
  val db = client(ServiceConfig.mongoDb)

  val serializers: PartialFunction[TypeHint, Grater[_ <: SessionEvent]] = sessionEventSerializers

  //  val subscriptions: List[Subscription] =
  //    AkkaSubscription[TokenCreatedEvent, GenerateSessionCommand](
  //      grater[TokenCreatedEvent], db, GlobalConfig.collectionsHost("UserEvent")){
  //      a ⇒
  //    } :: Nil

  val source: MongoSource[SessionEvent] =
    new MongoSource[SessionEvent](db, serializers)

  override def receive: Receive = {
    case GeneratedToken ⇒ log.error("OOPS!!!!!!")
  }
}
