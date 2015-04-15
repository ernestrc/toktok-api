package io.toktok.model

import krakken.model._

sealed trait SessionQuery extends Query
case class GetUserSession(userId: SID) extends SessionQuery
