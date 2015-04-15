package io.toktok.query.users

import java.util.concurrent.TimeUnit

import com.typesafe.config.{ConfigFactory, Config}

import scala.concurrent.duration.FiniteDuration

/**
 * Created by ernest on 4/11/15.
 */
object ServiceConfig {

  private val config: Config = ConfigFactory.load()

  val mongoHost = config.getString("toktok.source.host")

  val mongoDb = config.getString("toktok.source.db")

  val ACTOR_TIMEOUT: FiniteDuration =
    FiniteDuration(config.getDuration("toktok.actors.timeout",
      TimeUnit.SECONDS), TimeUnit.SECONDS)

  val ONLINE_THRESHOLD: FiniteDuration =
    FiniteDuration(config.getDuration("toktok.online-threshold",
      TimeUnit.SECONDS), TimeUnit.SECONDS)

}
