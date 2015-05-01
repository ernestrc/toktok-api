package io.toktok.analytics

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.routing.FromConfig
import akka.util.Timeout
import com.mongodb.casbah.MongoClient
import krakken.http.Endpoint
import krakken.utils.io._
import spray.routing.Route

class AnalyticsEndpoint(implicit val system: ActorSystem) extends Endpoint {

  val mongoContainer = getContainerLink(ApiConfig.dataContainer)
  val mongoHost: String =  mongoContainer.map(_.host.ip).getOrElse(ApiConfig.mongoHost)
  val mongoPort: Int = mongoContainer.map(_.port).getOrElse(ApiConfig.mongoPort)
  val dbName: String = ApiConfig.dbName
  val db = MongoClient(mongoHost, mongoPort)(dbName)

  import system.dispatcher

  implicit val timeout:Timeout = ApiConfig.ENDPOINT_TIMEOUT

  val persister: ActorRef = system.actorOf(Props(classOf[AnalyticsPersister], db)
    .withRouter(FromConfig()), "AnalyticsPersister")

  override def route: Route = {
    pathPrefix("v1"){
      path("analytics"){
        post{
          entity(as[String]) { str ⇒
            complete {
              persister.ask(str).mapTo[String]
            }
          }
        }
      }
    }
  }
}
