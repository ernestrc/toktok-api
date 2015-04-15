package io.toktok.query.users

import akka.actor.Props
import io.toktok.query.users.actors.{UserQueryGuardian, SessionQueryGuardian}
import krakken.MicroService

object Boot extends App {

  val actors: List[Props] = Props[SessionQueryGuardian] :: Props[UserQueryGuardian] :: Nil

  MicroService("users_query", actors)

}
