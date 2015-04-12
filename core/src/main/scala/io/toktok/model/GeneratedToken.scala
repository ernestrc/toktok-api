package io.toktok.model

import krakken.model.SID

/**
 * Created by ernest on 4/11/15.
 */
//TODO refactor messages out to model project
//TODO think about how to make the persistance of subscriptions seamslessly in views as well
//TODO design QueryActor[T]
case class GeneratedToken(token: String, userId: SID, sessionId: SID)
