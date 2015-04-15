package io.toktok.model

import krakken.model.{SID, Query}

sealed trait UserQuery extends Query
case class GetOnlineUsersQuery(requester: SID, users: List[SID])
