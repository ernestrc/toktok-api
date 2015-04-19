package io.toktok.notifications

import java.util.concurrent.TimeUnit

import com.typesafe.config.{ConfigFactory, Config}

import scala.concurrent.duration.FiniteDuration

object ServiceConfig {

  private val config: Config = ConfigFactory.load()

  val ACTOR_TIMEOUT: FiniteDuration =
    FiniteDuration(config.getDuration("toktok.actors.timeout",
      TimeUnit.SECONDS), TimeUnit.SECONDS)

  val RESET_RETRIES = config.getInt("toktok.actors.supervisor.retries")

  val RESET_WITHIN = FiniteDuration(config.getDuration("toktok.actors.supervisor.within",
    TimeUnit.SECONDS), TimeUnit.SECONDS)


}
