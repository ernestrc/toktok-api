package io.toktok.command.users.actors

import com.opentok.OpenTok
import io.toktok.model.{GenerateTokenCommand, SessionCreatedAnchor, SessionEvent, TokenCreatedEvent}
import krakken.dal.MongoSource
import krakken.model._
import krakken.system.EventSourcedCommandActor

class SessionCommandActor(anchor: SessionCreatedAnchor, val source: MongoSource[SessionEvent], opentok: OpenTok)
  extends EventSourcedCommandActor[SessionEvent] {

  override implicit val entityId: Option[SID] = Some(anchor.opentokSessionId)
  val sessionId: String = anchor.opentokSessionId

  override val subscriptions: List[Subscription] = List.empty

  override val eventProcessor: PartialFunction[Event, Unit] = {
    case TokenCreatedEvent(ses, tk) ⇒
      log.info(s"Token for session $ses was created")
  }

  override val commandProcessor: PartialFunction[Command, List[SessionEvent]] = {
    case _: GenerateTokenCommand ⇒
      TokenCreatedEvent(sessionId, opentok.generateToken(sessionId)) :: Nil
  }
}