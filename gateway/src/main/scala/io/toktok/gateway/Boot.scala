package io.toktok.gateway

import io.toktok.gateway.endpoints.{SessionEndpoint, InternalEndpoint, UserEndpoint}
import krakken.MicroService
import krakken.model.EndpointProps

object Boot extends App {

  val endpoints =
    EndpointProps[UserEndpoint] ::
//    EndpointProps[SessionEndpoint] ::
    EndpointProps[InternalEndpoint] :: Nil

  MicroService("gateway", ApiConfig.HOST, ApiConfig.PORT, List.empty, endpoints)

}
