package io.toktok.service.actors

import akka.actor.{Actor, ActorLogging}
import io.toktok.dal.MongoSource
import io.toktok.model.Exceptions.TokTokException
import io.toktok.model.{EventSubscription, Receipt}
import io.toktok.service.{Command, Event}
import io.toktok.utils.Implicits._

/**
 * Created by ernest on 4/2/15.
 */
abstract class EventSourcedActor[T <: Event] extends Actor with ActorLogging {

  override def postStop(): Unit = {
    subscriptions.foreach(_.unsubscribe())
  }

  val name: String = self.path.name

  val entityId: String

  val source: MongoSource[T]

  val subscriptions: List[EventSubscription]

  def subscriptionEventProcessor: PartialFunction[Event, List[T]]

  def eventProcessor: PartialFunction[T, Unit]

  def commandProcessor: PartialFunction[Command, List[T]]

  private def $processEvents(events: List[T]):Receipt = {
    try {
      events.foreach(source.save)
      events.foreach(eventProcessor)
      Receipt(success = true, updated = entityId, message = "OK")
    } catch {
      case ex: TokTokException ⇒
        Receipt.error(ex)
      case err: Exception ⇒
        log.error(err, s"There was an error when processing command/event!")
        Receipt.error(err)
    }
  }

  def receive: Receive = {
    case event: Event ⇒ sender() ! $processEvents(subscriptionEventProcessor(event))
      log.debug(s"Actor $name processed $event probably from a subscription")
    case cmd: Command ⇒ sender() ! $processEvents(commandProcessor(cmd))
      log.debug(s"Actor $name processed $cmd")
    case anyElse ⇒ log.error(s"Oops, it looks like I shouldn't have received $anyElse")
  }
}
