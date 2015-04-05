package service.actors

import akka.actor.{ActorLogging, Actor}
import dal.MongoSource
import model.{SID, Receipt}
import service.Event

import scala.util.Try

/**
 * Created by ernest on 4/2/15.
 */
trait EventSourcedActor[T <: Event] extends Actor with ActorLogging {

  val entityId: String
  val name: String = self.path.name

  def applyEvent: PartialFunction[T, Unit]

  def processCommand: PartialFunction[Any, List[T]]

  val source: MongoSource[T]

  def receive: Receive = {
    case any: Any ⇒
      val receipt = try {
        val events = processCommand.apply(any)
        events.foreach(source.save)
        events.foreach(applyEvent)
        Receipt(success = true, updated = entityId, message = "OK")
      } catch {
        case err: Exception ⇒ Receipt.error(err)
      }
      sender() ! receipt
      log.debug(s"Actor $name processed $any")
  }
}
