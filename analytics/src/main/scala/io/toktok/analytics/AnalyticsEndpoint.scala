package io.toktok.analytics

import akka.actor.{ActorSelection, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.routing.FromConfig
import akka.util.Timeout
import com.mongodb.casbah.MongoClient
import krakken.http.Endpoint
import krakken.io._
import spray.routing.Route

import scala.concurrent.Await
import scala.util.Try

class AnalyticsEndpoint(implicit val system: ActorSystem) extends Endpoint {

  import system.dispatcher

  implicit val timeout:Timeout = ApiConfig.ENDPOINT_TIMEOUT

  val persisterName = "AnalyticsPersister"
  val persister: ActorRef = system.actorOf(Props(classOf[AnalyticsPersister]), persisterName)
  val persistersPath: ActorSelection = system.actorSelection(system / persisterName / AnalyticsPersister.routerName)

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
