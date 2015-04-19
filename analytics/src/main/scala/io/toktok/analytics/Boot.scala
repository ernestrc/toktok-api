package io.toktok.analytics

import krakken.MicroService
import krakken.model.EndpointProps

object Boot extends App{

  val endpoints: List[EndpointProps] = EndpointProps[AnalyticsEndpoint] :: Nil

  MicroService("analytics", ApiConfig.HOST, ApiConfig.PORT, List.empty, endpoints)

}
