package io.toktok.gateway

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.FiniteDuration

/**
 * Created by ernest on 4/8/15.
 */
object ApiConfig {

  private val config: Config = ConfigFactory.load()

  val PORT = config.getInt("toktok.port")

  val HOST = config.getString("toktok.host")

  val ENDPOINT_TIMEOUT: FiniteDuration =
    FiniteDuration(config.getDuration("toktok.http.timeout",
      TimeUnit.SECONDS), TimeUnit.SECONDS)
  val ENDPOINT_FALLBACK_TIMEOUT: FiniteDuration =
    FiniteDuration(config.getDuration("toktok.actors.endpoint-fallback",
      TimeUnit.SECONDS), TimeUnit.SECONDS)

  val USERS_CMD_LOCATION:String = config.getString("toktok.services.command.users")
}
