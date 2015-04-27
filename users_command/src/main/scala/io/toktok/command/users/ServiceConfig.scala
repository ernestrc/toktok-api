package io.toktok.command.users

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}
import krakken.config.KrakkenConfig
import krakken.utils.io._

import scala.collection.convert.Wrappers
import scala.concurrent.duration.FiniteDuration
import scala.io._

object ServiceConfig extends KrakkenConfig {

  val dbName: String = config.getString("krakken.source.db")

  val mongoContainer = getContainerLink("mongo_command")

  val mongoHost: String = mongoContainer.map(_.host.ip).getOrElse {
    config.getString("krakken.source.host")
  }

  val mongoPort: Int = mongoContainer.map(_.port).getOrElse {
    config.getInt("krakken.source.port")
  }

  val RESET_RETRIES = config.getInt("toktok.actors.supervisor.retries")

  val RESET_WITHIN = FiniteDuration(config.getDuration("toktok.actors.supervisor.within",
    TimeUnit.SECONDS), TimeUnit.SECONDS)

  val OPENTOK_KEY = config.getInt("toktok.opentok.apikey")

  val OPENTOK_SECRET = config.getString("toktok.opentok.secret")

  val WHITELIST_EMAIL =
    Wrappers.JListWrapper(config.getStringList("toktok.email-whitelist")).toList

}
