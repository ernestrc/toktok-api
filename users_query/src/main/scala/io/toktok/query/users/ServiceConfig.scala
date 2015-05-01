package io.toktok.query.users

import java.util.concurrent.TimeUnit

import krakken.config.KrakkenConfig

import scala.concurrent.duration.FiniteDuration

/**
 * Created by ernest on 4/11/15.
 */
object ServiceConfig extends KrakkenConfig {

  val ONLINE_THRESHOLD: FiniteDuration =
    FiniteDuration(config.getDuration("toktok.online-threshold",
      TimeUnit.SECONDS), TimeUnit.SECONDS)

}
