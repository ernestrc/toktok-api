package io.toktok.model

import krakken.model.{View, SID}

sealed trait SessionView extends View
case class UserSession(userId: SID, sessionId: SID) extends SessionView