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

  val mongoHost: String = links.find(_.host.alias == "mongo_query")
    .map(_.host.ip).getOrElse {
    config.getString("krakken.source.host")
  }

  val mongoPort: Int = links.find(_.host.alias == "mongo_query")
    .map(_.port).getOrElse {
    config.getInt("krakken.source.port")
  }

  val dbName = config.getString("krakken.source.db")

  val RESET_RETRIES = config.getInt("toktok.actors.supervisor.retries")

  val RESET_WITHIN = FiniteDuration(config.getDuration("toktok.actors.supervisor.within",
    TimeUnit.SECONDS), TimeUnit.SECONDS)

}
