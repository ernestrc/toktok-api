package io.toktok.analytics

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.routing.FromConfig
import akka.util.Timeout
import com.mongodb.DBObject
import com.mongodb.casbah._
import krakken.io.{DiscoveryActor, Service}
import krakken.utils.Implicits._
import akka.pattern._
import spray.json._

import scala.concurrent.Await
import scala.util.Try

/**
 * Created by ernest on 4/18/15.
 */
class AnalyticsPersister extends Actor with ActorLogging {

  import context.dispatcher

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(loggingEnabled = true, maxNrOfRetries = -1) {
      case e: DeathPactException ⇒ akka.actor.SupervisorStrategy.Escalate
      case e: akka.actor.ActorInitializationException ⇒ log.error(e, s"${e.getCause}"); Restart
  }

  implicit val timeout: Timeout = ApiConfig.ACTOR_TIMEOUT

  val discoveryActor = context.actorOf(Props[DiscoveryActor])
  var analyticsCol: MongoCollection = null

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.info(s"Starting ${this.getClass.getSimpleName} configuration")
    val mongoContainer: Option[Service] = Try(Await.result(discoveryActor.ask(DiscoveryActor.Find(ApiConfig.dataContainer)).mapTo[Service], ApiConfig.ACTOR_TIMEOUT)).toOption
    val mongoHost: String = mongoContainer.map(_.host.ip).getOrElse(ApiConfig.mongoHost)
    val mongoPort: Int = mongoContainer.map(_.port).getOrElse(ApiConfig.mongoPort)
    val dbName: String = ApiConfig.dbName
    val db = MongoClient(mongoHost, mongoPort)(dbName)
    analyticsCol = db(ApiConfig.analyticsColl)
    //instantiate router + routees
    context.actorOf(Props(classOf[AnalyticsWorker], analyticsCol)
      .withRouter(FromConfig()), AnalyticsPersister.routerName)

    //check connectivity
    db.collectionNames()
    log.info(s"${this.getClass.getSimpleName} actor is up and running. Analytics coll is $analyticsCol!")
  }


  @throws[Exception](classOf[Exception])
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    context.children.foreach(context.stop)
  }

  override def receive: Receive = {
    case "" ⇒
  }
}

object AnalyticsPersister {
  val routerName = "router"
}

class AnalyticsWorker(analyticsCol: MongoCollection) extends Actor with ActorLogging {

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.info("Persister {} is up and running", self.path)
  }


  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    log.info("Persister {} is terminating", self.path)
  }

  override def receive: Receive = {
    case o: String ⇒
      val bson: DBObject = o.parseJson.asJsObject
      val w = analyticsCol.save(bson)
      log.debug("Received, parsed and saved item")
      sender() ! "OK"
  }
}