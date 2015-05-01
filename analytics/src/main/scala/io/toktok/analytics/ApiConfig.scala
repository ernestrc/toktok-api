package io.toktok.analytics

import java.util.concurrent.TimeUnit

import krakken.config.KrakkenConfig
import krakken.utils.io._

import scala.concurrent.duration.FiniteDuration

/**
 * Created by ernest on 4/18/15.
 */
object ApiConfig extends KrakkenConfig {

  val analyticsColl = config.getString("toktok.analytics-coll")

  val PORT = config.getInt("toktok.port")

  val HOST = config.getString("toktok.host")

  val ENDPOINT_TIMEOUT: FiniteDuration =
    FiniteDuration(config.getDuration("toktok.http.timeout",
      TimeUnit.SECONDS), TimeUnit.SECONDS)
}
