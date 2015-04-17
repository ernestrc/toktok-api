package io.toktok.query.users.actors

import com.novus.salat.Grater
import io.toktok.model.{GetUserSession, SessionCreatedAnchor, SessionEvent, UserSession}
import krakken.model._
import krakken.system.EventSourcedQueryActor

/**
* Created by ernest on 4/12/15.
*/
class SessionQueryActor(anchor: SessionCreatedAnchor) extends EventSourcedQueryActor[SessionEvent] {


  override val subscriptionSerializers: FromHintGrater[AnyRef] =
    PartialFunction.empty[TypeHint, Grater[_ <: SessionEvent]]

  override implicit val entityId: Option[SID] = Some(anchor.userId)
  val sessionId = anchor.opentokSessionId

  override val subscriptions: List[Subscription] = List.empty

  override val eventProcessor: PartialFunction[Event, Unit] = {
    case _ ⇒
  }

  override val queryProcessor :PartialFunction[Query, View] = {
    case cmd @ GetUserSession(userId) ⇒ UserSession(anchor.userId, sessionId)
  }
}
