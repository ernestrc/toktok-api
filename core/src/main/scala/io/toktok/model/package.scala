package io.toktok

import com.novus.salat.Grater
import krakken.macros.Macros._
import krakken.model.FromHintGrater
import krakken.model.ctx

/**
 * Created by ernest on 4/11/15.
 */
package object model {

  implicit val sessionEventSerializers: FromHintGrater[SessionEvent] = grateSealed[SessionEvent]

  implicit val notificationEventSerializers: FromHintGrater[NotificationEvent] = grateSealed[NotificationEvent]

  implicit val userEventSerializers: FromHintGrater[UserEvent] = grateSealed[UserEvent]

}
