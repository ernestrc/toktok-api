package io.toktok.analytics

import akka.actor.{Actor, ActorLogging}
import com.mongodb.DBObject
import com.mongodb.casbah._
import krakken.utils.Implicits._
import spray.json._

/**
 * Created by ernest on 4/18/15.
 */
class AnalyticsPersister(db: MongoDB) extends Actor with ActorLogging {


  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.info("Persister {} is up and running!", self.path)
  }


  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    log.info("Persister {} is terminating", self.path)
  }

  val analyticsCol = db(ApiConfig.analyticsColl)

  override def receive: Receive = {
    case o: String ⇒
      val bson: DBObject = o.parseJson.asJsObject
      val w = analyticsCol.save(bson)
      log.debug("Received and saved {}", bson)
      sender() ! "OK"
  }
}
