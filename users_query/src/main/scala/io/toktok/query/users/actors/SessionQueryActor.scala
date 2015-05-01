package io.toktok.query.users.actors

import com.mongodb.casbah.{MongoDB, MongoClient, Imports}
import com.novus.salat.Grater
import io.toktok.model.{GetUserSession, SessionCreatedAnchor, SessionEvent, UserSession}
import io.toktok.query.users.ServiceConfig
import krakken.model._
import krakken.system.EventSourcedQueryActor


class SessionQueryActor(anchor: SessionCreatedAnchor) extends EventSourcedQueryActor[SessionEvent] {

  override val subscriptionSerializers: FromHintGrater[AnyRef] =
    PartialFunction.empty[TypeHint, Grater[_ <: SessionEvent]]

  override implicit val entityId: Option[SID] = Some(anchor.userId)
  val sessionId = anchor.opentokSessionId

  override val subscriptions: List[Subscription] = List.empty

  override val eventProcessor: PartialFunction[Event, Unit] = {
    case _ ⇒
  }

  override val queryProcessor: PartialFunction[Query, View] = {
    case cmd@GetUserSession(userId) ⇒ UserSession(anchor.userId, sessionId)
  }
}
