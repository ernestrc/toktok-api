package io.toktok.command.users.actors

import com.opentok.OpenTok
import io.toktok.model.{GenerateTokenCommand, SessionCreatedAnchor, SessionEvent, TokenCreatedEvent}
import krakken.dal.MongoSource
import krakken.model._
import krakken.service.EventSourcedActor

class SessionActor(anchor: SessionCreatedAnchor, val source: MongoSource[SessionEvent], opentok: OpenTok)
  extends EventSourcedActor[SessionEvent] {

  override implicit val entityId: Option[SID] = Some(anchor.userId)
  val sessionId: String = anchor.opentokSessionId

  override val subscriptions: List[Subscription] = List.empty

  override val eventProcessor: PartialFunction[Event, Unit] = {
    case TokenCreatedEvent(userId, ses, tk) ⇒
      log.info(s"Token for user $userId session $ses was created")
  }

  override val commandProcessor: PartialFunction[Command, List[SessionEvent]] = {
    case _: GenerateTokenCommand ⇒
      TokenCreatedEvent(entityId.get, sessionId, opentok.generateToken(sessionId)) :: Nil
  }
}