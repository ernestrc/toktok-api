package io.toktok.analytics

import akka.actor.Props
import krakken.MicroService
import krakken.http.EndpointProps
import krakken.io.{Service, DiscoveryActor}

object Boot extends App{

  val endpoints: List[EndpointProps] = EndpointProps[AnalyticsEndpoint] :: Nil

  MicroService("analytics", ApiConfig.HOST, ApiConfig.PORT, List.empty, endpoints)

}
