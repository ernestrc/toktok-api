package io.toktok.command.users

import java.util.concurrent.TimeUnit

import krakken.config.KrakkenConfig

import scala.collection.convert.Wrappers
import scala.concurrent.duration.FiniteDuration

object ServiceConfig extends KrakkenConfig {

  val RESET_RETRIES = config.getInt("toktok.actors.supervisor.retries")

  val RESET_WITHIN = FiniteDuration(config.getDuration("toktok.actors.supervisor.within",
    TimeUnit.SECONDS), TimeUnit.SECONDS)

  val OPENTOK_KEY = config.getInt("toktok.opentok.apikey")

  val OPENTOK_SECRET = config.getString("toktok.opentok.secret")

  val WHITELIST_EMAIL =
    Wrappers.JListWrapper(config.getStringList("toktok.email-whitelist")).toList

}
