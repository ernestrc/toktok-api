package io.toktok.query.users.actors

import io.toktok.model.{UserSession, GetUserSession, SessionCreatedAnchor, SessionEvent}
import krakken.dal.MongoSource
import krakken.model._
import krakken.system.EventSourcedQueryActor

/**
* Created by ernest on 4/12/15.
*/
class SessionQueryActor(anchor: SessionCreatedAnchor, val source: MongoSource[SessionEvent]) extends EventSourcedQueryActor[SessionEvent] {

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
