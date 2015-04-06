package io.toktok.model

import akka.actor._
import com.mongodb.casbah.Imports._
import com.mongodb.{BasicDBObjectBuilder, Bytes, DBCursor}
import com.novus.salat.Grater
import io.toktok.config.GlobalConfig
import io.toktok.model.SubscriptionMaster.{CursorEmpty, DispatchedEvent, Subscribe, Unsubscribe}
import io.toktok.service.Event
import model.SID
import org.bson.types.BSONTimestamp

import scala.reflect.ClassTag

trait EventSubscription {
  def unsubscribe(): Unit
}

//TODO throw exception when non-replicaSet
case class AkkaEventSubscription[A <: B : ClassTag, B <: Event : ClassTag]
(subscriber: ActorRef, serializer: Grater[A], entityId: SID)(implicit context: ActorContext) extends EventSubscription {

  val subscribedTo = implicitly[ClassTag[A]].runtimeClass
  val typeHint = subscribedTo.getCanonicalName
  val parentType = implicitly[ClassTag[B]].runtimeClass.getSimpleName
  val eventSourceHost: String = GlobalConfig.collectionsHost(parentType)

  val master = context.actorOf(Props(classOf[SubscriptionMaster[A]],
    subscriber, typeHint, eventSourceHost, serializer, entityId))

  def unsubscribe(): Unit = master ! Unsubscribe

}

object SubscriptionMaster {

  case object Unsubscribe
  case class Subscribe(cursor: DBCursor)
  case class DispatchedEvent(timestamp: BSONTimestamp)
  case object CursorEmpty

}

class SubscriptionMaster[A <: Event](
  subscriber: ActorRef,
  typeHint: String,
  eventSourceHost: String,
  serializer: Grater[A],
  entityId: SID
) extends Actor with ActorLogging {

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    lastTs = opLog.underlying.find()
      .sort(BasicDBObjectBuilder.start("ts", -1).get())
      .limit(1).one().get("ts").asInstanceOf[BSONTimestamp]
    log.info(s"Instantiated subscription mechanism on $typeHint in $eventSourceHost successfully")
    log.debug(s"Last object inserted was on $lastTs")
    worker ! Subscribe(generateCursor(lastTs))
  }

  val client = MongoClient(eventSourceHost)
  val db = client("local")
  val opLog = db("oplog.rs")

  var lastTs: BSONTimestamp = null
  val worker = context.actorOf(
    Props(classOf[SubscriptionWorker[A]], subscriber, serializer))

  def generateCursor(l: BSONTimestamp): DBCursor = {
    val query = MongoDBObject(
      "ts" → MongoDBObject("$gt" → l),
      "o._typeHint" → typeHint,
      "o.entityId" → entityId
    )
    val sort = BasicDBObjectBuilder.start("$natural", 1).get()

    opLog.underlying
      .find(query)
      .sort(sort)
      .addOption(Bytes.QUERYOPTION_TAILABLE)
      .addOption(Bytes.QUERYOPTION_AWAITDATA)
  }

  override def receive: Actor.Receive = {
    case CursorEmpty ⇒
      sender() ! Subscribe(generateCursor(lastTs))
      log.debug(s"Cursor exhausted. Dispatched a new one")
    case DispatchedEvent(timestamp) ⇒
      lastTs = timestamp
      log.debug(s"Subscription dispatched event!")
    case Unsubscribe ⇒ context.stop(self)
  }
}

class SubscriptionWorker[A <: AnyRef](subscriber: ActorRef, serializer: Grater[A]) extends Actor with ActorLogging {

  override def postStop(): Unit = {
    if (currentCursor != null) currentCursor.close()
  }

  var currentCursor: DBCursor = null

  def subscribe(cursor: DBCursor): Unit = {
    currentCursor = cursor
    while (cursor.hasNext) {
      val dbObjectEvent = cursor.next()
      val ts = dbObjectEvent.as[BSONTimestamp]("ts")
      val event = serializer.asObject(dbObjectEvent.get("o").asInstanceOf[DBObject])
      subscriber ! event
      context.parent ! DispatchedEvent(ts)
    }
    context.parent ! CursorEmpty
    cursor.close()
  }

  override def preStart(): Unit = {
    log.debug(s"Subscription worker in ${self.path} is ready to rock")
  }

  override def receive: Receive = {
    case Subscribe(cur) ⇒
      log.debug(s"Using tailable cursor $cur...")
      subscribe(cur)
  }
}
