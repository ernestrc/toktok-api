//package io.toktok.query.users.actors
//
//import io.toktok.model.{TokenCreatedEvent, SessionCreatedAnchor, SessionEvent}
//import krakken.config.GlobalConfig
//import krakken.dal.MongoSource
//import krakken.model._
//import krakken.system.EventSourcedQueryActor
//import com.novus.salat._
//import com.novus.salat.global.ctx
//
///**
// * Created by ernest on 4/12/15.
// */
//class SessionQueryActor(anchor: SessionCreatedAnchor, val source: MongoSource[SessionEvent]) extends EventSourcedQueryActor[SessionEvent] {
//
//  override implicit val entityId: Option[SID] = Some(anchor.userId)
//
//  override val subscriptions: List[Subscription] =
//    AkkaSubscription.forView[TokenCreatedEvent](grater[TokenCreatedEvent],
//      source.db, GlobalConfig.collectionsHost("SessionEvent"),
//      GlobalConfig.collectionsDB("SessionEvent")) :: Nil
//
//  override val eventProcessor: PartialFunction[Event, Unit] = {
//    case TokenCreatedEvent(userId, sessionId, token) ⇒
//  }
//
//  override val queryProcessor: PartialFunction[Query, View] = {
//
//  }
//}
