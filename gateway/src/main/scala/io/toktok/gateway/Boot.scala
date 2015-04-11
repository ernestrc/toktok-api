package io.toktok.gateway

import io.toktok.gateway.endpoints.{InternalEndpoint, UserEndpoint}
import krakken.MicroService
import krakken.model.EndpointProps

object Boot extends App {

  val endpoints = EndpointProps[UserEndpoint] :: EndpointProps[InternalEndpoint] :: Nil

  MicroService("gateway", ApiConfig.HOST, ApiConfig.PORT, List.empty, endpoints)

}
