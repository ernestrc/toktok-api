package io.toktok.command.users


import akka.actor.{OneForOneStrategy, SupervisorStrategyConfigurator}

class UserGuardianStrategy extends SupervisorStrategyConfigurator {
  def create() = OneForOneStrategy(ServiceConfig.RESET_RETRIES, ServiceConfig.RESET_WITHIN, loggingEnabled = true) {
    case e:Exception ⇒ akka.actor.SupervisorStrategy.Restart
  }
}
