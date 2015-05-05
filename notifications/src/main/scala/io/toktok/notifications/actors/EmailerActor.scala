package io.toktok.notifications.actors

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.novus.salat._
import com.postmark.{Message, PostmarkActor}
import io.toktok.model.{UserEvent, _}
import io.toktok.notifications.ServiceConfig
import krakken.dal.{AkkaSubscription, Subscription}
import krakken.io._
import krakken.model.{ctx, _}
import krakken.system.EventSourcedCommandActor
import play.twirl.api.Html

import scala.concurrent.{Await, Future}
import scala.util.Try

class EmailerActor extends EventSourcedCommandActor[NotificationEvent] {

  val userEvent = classOf[UserEvent].getSimpleName

  override def preStart(): Unit = {
    val mongoContainer: Option[Service] = Try(Await.result(discoveryActor.ask(
      DiscoveryActor.Find(ServiceConfig.collectionsSourceContainer(userEvent)))(ServiceConfig.ACTOR_TIMEOUT)
      .mapTo[Service], ServiceConfig.ACTOR_TIMEOUT))
      .toOption
    val mongoHost: String =  mongoContainer.map(_.host.ip).getOrElse(ServiceConfig.collectionsConfigHost(userEvent))
    val mongoPort: Int = mongoContainer.map(_.port).getOrElse(ServiceConfig.collectionsPort(userEvent))
      userEventSourceHost = Some(mongoHost)
      userEventSourcePort = Some(mongoPort)
      super.preStart()
  }

  implicit val timeout: Timeout = ServiceConfig.ACTOR_TIMEOUT

  override implicit val entityId: Option[SID] = None

  val postmarkActor: ActorRef = context.actorOf(Props[PostmarkActor])

  var userEventSourceHost: Option[String] = None
  var userEventSourcePort: Option[Int] = None

  override lazy val subscriptions: List[Subscription] =
    AkkaSubscription[UserCreatedAnchor, SendActivationEmailCommand](
      grater[UserCreatedAnchor], db, userEventSourceHost.get, userEventSourcePort.get,
      ServiceConfig.collectionsDB(userEvent)) {
      a ⇒ SendActivationEmailCommand(a.uuid.get.toString, a.username, a.email)
    } :: AkkaSubscription[SendNewPasswordEvent, SendNewPasswordCommand](
      grater[SendNewPasswordEvent], db, userEventSourceHost.get, userEventSourcePort.get,
      ServiceConfig.collectionsDB(userEvent)) {
      a ⇒ SendNewPasswordCommand(a.entityId, a.newPassword, a.email)
    } :: Nil

  var sent: Int = 0

  override val eventProcessor: PartialFunction[Event, Unit] = {
    case ActivationEmailSendEvent(id, email) ⇒ sent += 1
    case NewPasswordEmailSent(id) ⇒ sent += 1
  }

  def sendEmail(html: Html, subject: String, email: String): Future[Message.Receipt] = {
    val msg = Message.Builder()
      .from("info@toktok.io")
      .htmlBody(html.toString()).to(email)
      .subject(subject)
      .build
    postmarkActor.ask(msg).mapTo[Message.Receipt]
  }

  val anchorGrater = grater[UserCreatedAnchor]

  override val commandProcessor: PartialFunction[Command, List[NotificationEvent]] = {
    case SendNewPasswordCommand(userId, newPassword, email) ⇒
      Await.result(
        sendEmail(html.forgot(newPassword), "Forgot Password", email),
        ServiceConfig.ACTOR_TIMEOUT) match {
        case m if m.ErrorCode == 0 ⇒ NewPasswordEmailSent(userId) :: Nil
      }
    case SendActivationEmailCommand(userId, username, email) ⇒
      Await.result(
        sendEmail(html.activate(userId, username, ServiceConfig.activationUrl), "Activate your Toktok.io account", email),
        ServiceConfig.ACTOR_TIMEOUT) match {
        case m if m.ErrorCode == 0 ⇒ ActivationEmailSendEvent(userId, email) :: Nil
      }
  }
}
