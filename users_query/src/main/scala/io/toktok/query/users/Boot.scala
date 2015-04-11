package io.toktok.query.users

import akka.actor.Props
import io.toktok.query.users.actors.SessionActor
import krakken.MicroService

object Boot extends App {

  val actors: List[Props] = Props[SessionActor] :: Nil

  MicroService("users_query", actors)

}
