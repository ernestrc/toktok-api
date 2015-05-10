package io.toktok.analytics

import akka.actor._
import akka.pattern.ask
import akka.routing.FromConfig
import akka.util.Timeout
import com.mongodb.casbah.MongoClient
import krakken.http.Endpoint
import krakken.io._
import spray.routing.Route

import scala.concurrent.Await
import scala.util.Try

class AnalyticsEndpoint(implicit val context: ActorContext) extends Endpoint {

  import context.dispatcher

  implicit val timeout:Timeout = ApiConfig.ENDPOINT_TIMEOUT

  val persisterName = "AnalyticsPersister"
  val persister: ActorRef = context.actorOf(Props(classOf[AnalyticsPersister]), persisterName)
  val persistersPath: ActorSelection = context.actorSelection(context.system / "http"/ "handler"/ persisterName / AnalyticsPersister.routerName)

  override def route: Route = {
    pathPrefix("v1"){
      path("analytics"){
        post{
          entity(as[String]) { str ⇒
            complete {
              persistersPath.ask(str).mapTo[String]
            }
          }
        }
      }
    }
  }
}
