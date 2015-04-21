package io.toktok.gateway

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}
import krakken.config.KrakkenConfig

import scala.concurrent.duration.FiniteDuration

/**
 * Created by ernest on 4/8/15.
 */
object ApiConfig extends KrakkenConfig {

  val PORT = config.getInt("toktok.port")

  val HOST = config.getString("toktok.host")

  val ENDPOINT_TIMEOUT: FiniteDuration =
    FiniteDuration(config.getDuration("toktok.http.timeout",
      TimeUnit.SECONDS), TimeUnit.SECONDS)

  val ENDPOINT_FALLBACK_TIMEOUT: FiniteDuration =
    FiniteDuration(config.getDuration("toktok.actors.endpoint-fallback",
      TimeUnit.SECONDS), TimeUnit.SECONDS)
  
  val USERS_CMD_LOCATION:String = links.find(_.host.alias == "users_command")
    .map(_.toAkkaUrl)
    .getOrElse(config.getString("krakken.services.command.users"))

  val USERS_QUERY_LOCATION:String = links.find(_.host.alias == "users_query")
    .map(_.toAkkaUrl)
    .getOrElse(config.getString("krakken.services.query.users"))
}
