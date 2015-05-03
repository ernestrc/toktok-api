package io.toktok.analytics

import akka.actor.{AllForOneStrategy, DeathPactException, SupervisorStrategyConfigurator}

class AnalyticsGuardianStrategy extends SupervisorStrategyConfigurator{
  def create() = AllForOneStrategy(loggingEnabled = true) {
    case e: DeathPactException ⇒ akka.actor.SupervisorStrategy.Restart
    case e:Exception ⇒ akka.actor.SupervisorStrategy.Restart
  }
}
