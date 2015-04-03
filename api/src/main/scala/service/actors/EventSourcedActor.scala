package service.actors

import akka.actor.{ActorLogging, Actor}
import model.Receipt
import service.Event

/**
 * Created by ernest on 4/2/15.
 */
trait EventSourcedActor extends Actor with ActorLogging {
  def applyEvent: PartialFunction[_ <: Event, Unit]
  def processCommand: PartialFunction[Any, Unit]
  def receive = processCommand
}
