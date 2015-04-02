package config

import java.util.concurrent.TimeUnit

import com.novus.salat.Context
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.FiniteDuration

object GlobalConfig {

  private val config: Config = ConfigFactory.load()

  val mongoHost = config.getString("toktok.source.host")

  val mongoDb = config.getString("toktok.source.db")

  val PORT = config.getInt("toktok.port")

  val HOST = config.getString("toktok.host")

  val OPENTOK_KEY = config.getString("toktok.opentok.apikey")

  val OPENTOK_SECRET = config.getString("toktok.opentok.secret")

  val ENDPOINT_TIMEOUT: FiniteDuration =
    FiniteDuration(config.getDuration("toktok.http.timeout",
      TimeUnit.SECONDS), TimeUnit.SECONDS)

  val ACTOR_TIMEOUT: FiniteDuration =
    FiniteDuration(config.getDuration("toktok.actors.timeout",
      TimeUnit.SECONDS), TimeUnit.SECONDS)

}
