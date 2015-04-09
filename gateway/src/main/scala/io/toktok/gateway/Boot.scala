package io.toktok.gateway

import io.toktok.gateway.endpoints.{UserEndpoint, InternalEndpoint}
import io.toktok.model.{MicroService, EndpointProps}
import io.toktok.service.BootedSystem

object Boot extends App {

  val endpoints = EndpointProps[UserEndpoint] :: EndpointProps[InternalEndpoint] :: Nil

  MicroService("gateway", ApiConfig.HOST, ApiConfig.PORT, List.empty, endpoints)

}
