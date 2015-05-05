package io.toktok.command.users.actors

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.novus.salat._
import com.opentok.{MediaMode, OpenTok, SessionProperties}
import io.toktok.command.users.ServiceConfig
import io.toktok.model._
import krakken.dal.{AkkaSubscription, Subscription}
import krakken.io._
import krakken.model.{ctx, _}
import krakken.system.EventSourcedCommandActor

import scala.concurrent.Await
import scala.util.Try


class SessionCommandGuardian extends EventSourcedCommandActor[SessionEvent] {

  def newChild(anchor: SessionCreatedAnchor) = {
    context.actorOf(Props(classOf[SessionCommandActor], anchor, opentok), anchor.opentokSessionId)
  }

  val userEvent = classOf[UserEvent].getSimpleName

  override def preStart(): Unit = {
    log.info(s"SessionGuardian is up and running in path ${self.path}")
    source.findAllEventsOfType[SessionCreatedAnchor].foreach { anchor ⇒
      log.info(s"Booting up session actor for user ${anchor.userId}")
      newChild(anchor)
    }
    val mongoContainer: Option[Service] = Try(Await.result(discoveryActor.ask(
      DiscoveryActor.Find(ServiceConfig.collectionsSourceContainer(userEvent)))(ServiceConfig.ACTOR_TIMEOUT)
      .mapTo[Service], ServiceConfig.ACTOR_TIMEOUT))
      .toOption
    val mongoHost: String =  mongoContainer.map(_.host.ip).getOrElse(ServiceConfig.collectionsConfigHost(userEvent))
    val mongoPort: Int = mongoContainer.map(_.port).getOrElse(ServiceConfig.collectionsPort(userEvent))
    userEventSourceHost = Some(mongoHost)
    userEventSourcePort = Some(mongoPort)
    subscriptions.foreach(_.subscribe())
  }

  implicit val timeout: Timeout = ServiceConfig.ACTOR_TIMEOUT
  override implicit val entityId: Option[SID] = None
  val opentok = new OpenTok(ServiceConfig.OPENTOK_KEY, ServiceConfig.OPENTOK_SECRET)

  override val eventProcessor: PartialFunction[Event, Unit] = {
    case anchor: SessionCreatedAnchor ⇒ newChild(anchor)
  }

  override val commandProcessor: PartialFunction[Command, List[SessionEvent]] = {
    case GenerateSessionCommand(id) ⇒
      val properties = new SessionProperties.Builder().mediaMode(MediaMode.ROUTED).build()
      val ses = opentok.createSession(properties)
      SessionCreatedAnchor(id, ses.getSessionId) :: Nil
  }

  var userEventSourceHost: Option[String] = None
  var userEventSourcePort: Option[Int] = None

  lazy val subscriptions: List[Subscription] =
    AkkaSubscription[UserActivatedEvent, GenerateSessionCommand](
      grater[UserActivatedEvent], db, userEventSourceHost.get, userEventSourcePort.get,
      ServiceConfig.collectionsDB(userEvent)) {
      a ⇒ GenerateSessionCommand(a.entityId)
    } :: Nil
}