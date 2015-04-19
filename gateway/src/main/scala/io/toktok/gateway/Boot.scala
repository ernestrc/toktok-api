package io.toktok.gateway

import io.toktok.gateway.endpoints.{SessionEndpoint, UserEndpoint}
import krakken.MicroService
import krakken.model.EndpointProps

object Boot extends App {

  val endpoints =
    EndpointProps[UserEndpoint] ::
    EndpointProps[SessionEndpoint] :: Nil

  MicroService("gateway", ApiConfig.HOST, ApiConfig.PORT, List.empty, endpoints)

}
