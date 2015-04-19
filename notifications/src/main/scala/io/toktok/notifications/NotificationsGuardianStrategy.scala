package io.toktok.notifications

import akka.actor.{SupervisorStrategyConfigurator, OneForOneStrategy}

/**
 * Created by ernest on 4/18/15.
 */
class NotificationsGuardianStrategy extends SupervisorStrategyConfigurator{
  def create() = OneForOneStrategy(ServiceConfig.RESET_RETRIES, ServiceConfig.RESET_WITHIN, loggingEnabled = true) {
    case e:Exception ⇒ akka.actor.SupervisorStrategy.Restart
  }
}
