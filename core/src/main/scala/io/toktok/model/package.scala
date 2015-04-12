package io.toktok

import com.novus.salat.Grater
import krakken.macros.Macros._
import krakken.model.TypeHint
import com.novus.salat.global.ctx

/**
 * Created by ernest on 4/11/15.
 */
package object model {

  val sessionEventSerializers: PartialFunction[TypeHint, Grater[_ <: SessionEvent]] = grateSealed[SessionEvent]

  val userEventSerializers: PartialFunction[TypeHint, Grater[_ <: UserEvent]] = grateSealed[UserEvent]

}
