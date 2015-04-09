package io.toktok.command.users

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.convert.Wrappers
import scala.concurrent.duration.FiniteDuration

object ServiceConfig {

  private val config: Config = ConfigFactory.load()

  val mongoHost = config.getString("toktok.source.host")

  val mongoDb = config.getString("toktok.source.db")

  def collectionsHost(collection: String) = config.getString(s"toktok.source.collections.$collection.host")

  val OPENTOK_KEY = config.getInt("toktok.opentok.apikey")

  val OPENTOK_SECRET = config.getString("toktok.opentok.secret")

  val ACTOR_TIMEOUT: FiniteDuration =
  FiniteDuration(config.getDuration("toktok.actors.timeout",
  TimeUnit.SECONDS), TimeUnit.SECONDS)

  val WHITELIST_EMAIL =
    Wrappers.JListWrapper(config.getStringList("toktok.email-whitelist")).toList

}
