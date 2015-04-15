package io.toktok.model

import krakken.model.SID

sealed trait UserView
case class OnlineUsers(users: List[SID]) extends UserView