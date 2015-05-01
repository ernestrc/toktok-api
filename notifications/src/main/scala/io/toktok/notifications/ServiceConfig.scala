package io.toktok.notifications

import java.util.concurrent.TimeUnit

import krakken.config.KrakkenConfig

import scala.concurrent.duration.FiniteDuration

object ServiceConfig extends KrakkenConfig {

  val activationUrl: String = {
    val a = config.getString("toktok.activation-url")
    if (a.endsWith("/")) a.dropRight(1)
    else a
  }

  val RESET_RETRIES = config.getInt("toktok.actors.supervisor.retries")

  val RESET_WITHIN = FiniteDuration(config.getDuration("toktok.actors.supervisor.within",
    TimeUnit.SECONDS), TimeUnit.SECONDS)

}
