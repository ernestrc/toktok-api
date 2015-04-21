package io.toktok.query.users

import java.util.concurrent.TimeUnit

import krakken.config.KrakkenConfig

import scala.concurrent.duration.FiniteDuration

/**
 * Created by ernest on 4/11/15.
 */
object ServiceConfig extends KrakkenConfig {

  val mongoHost: String = links.find(_.host.alias == "mongo_query")
    .map(_.host.ip).getOrElse {
    config.getString("krakken.source.host")
  }

  val mongoPort: Int = links.find(_.host.alias == "mongo_query")
    .map(_.port).getOrElse {
    config.getInt("krakken.source.port")
  }
  
  val dbName = config.getString("krakken.source.db")

  val ONLINE_THRESHOLD: FiniteDuration =
    FiniteDuration(config.getDuration("toktok.online-threshold",
      TimeUnit.SECONDS), TimeUnit.SECONDS)

}
