package io.toktok.analytics

import java.util.concurrent.TimeUnit

import krakken.config.KrakkenConfig

import scala.concurrent.duration.FiniteDuration

/**
 * Created by ernest on 4/18/15.
 */
object ApiConfig extends KrakkenConfig {

  val mongoHost: String = links.find(_.host.alias == "mongo_query")
    .map(_.host.ip).getOrElse {
    config.getString("krakken.source.host")
  }

  val mongoPort: Int = links.find(_.host.alias == "mongo_query")
    .map(_.port).getOrElse {
    config.getInt("krakken.source.port")
  }

  val dbName = config.getString("krakken.source.db")

  val analyticsColl = config.getString("toktok.analytics-coll")

  val PORT = config.getInt("toktok.port")

  val HOST = config.getString("toktok.host")

  val ENDPOINT_TIMEOUT: FiniteDuration =
    FiniteDuration(config.getDuration("toktok.http.timeout",
      TimeUnit.SECONDS), TimeUnit.SECONDS)
}
