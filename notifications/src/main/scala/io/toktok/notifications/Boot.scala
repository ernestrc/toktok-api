package io.toktok.notifications

import akka.actor.Props
import io.toktok.notifications.actors.EmailerActor
import krakken.MicroService
import krakken.model.EndpointProps

object Boot extends App {

  val actors = Props[EmailerActor] :: Nil
  /*val endpoints = EndpointProps[InternalEndpoint] :: Nil*/

  MicroService("notifications", /*"127.0.0.1", 2899,*/ actors/*, endpoints*/)
}
