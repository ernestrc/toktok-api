package io.toktok.notifications.actors

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.mongodb.casbah.MongoClient
import com.novus.salat._
import krakken.model.ctx
import com.postmark.{Message, PostmarkActor}
import io.toktok.model.{UserEvent, _}
import io.toktok.notifications.ServiceConfig
import krakken.config.KrakkenConfig
import krakken.dal.MongoSource
import krakken.model._
import krakken.system.EventSourcedCommandActor
import play.twirl.api.Html

import scala.concurrent.{Future, Await}

class EmailerActor extends EventSourcedCommandActor[NotificationEvent] {

  implicit val timeout: Timeout = ServiceConfig.ACTOR_TIMEOUT

  override implicit val entityId: Option[SID] = None

  val postmarkActor: ActorRef = context.actorOf(Props[PostmarkActor])

  val db = MongoClient(ServiceConfig.mongoHost,
    ServiceConfig.mongoPort)(ServiceConfig.dbName)

  override val subscriptions: List[Subscription] =
    AkkaSubscription[UserCreatedAnchor, SendActivationEmailCommand](
      grater[UserCreatedAnchor], db, ServiceConfig.collectionsHost(classOf[UserEvent].getSimpleName),
      ServiceConfig.collectionsDB(classOf[UserEvent].getSimpleName)) {
      a ⇒ SendActivationEmailCommand(a.uuid.get.toString, a.username, a.email)
    } :: AkkaSubscription[SendNewPasswordEvent, SendNewPasswordCommand](
      grater[SendNewPasswordEvent], db, ServiceConfig.collectionsHost(classOf[UserEvent].getSimpleName),
      ServiceConfig.collectionsDB(classOf[UserEvent].getSimpleName)) {
      a ⇒ SendNewPasswordCommand(a.entityId, a.newPassword, a.email)
    } :: Nil

  override val source: MongoSource[NotificationEvent] =
    new MongoSource[NotificationEvent](db)

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
        sendEmail(html.activate(userId, username), "Activate your Toktok.io account", email),
        ServiceConfig.ACTOR_TIMEOUT) match {
        case m if m.ErrorCode == 0 ⇒ ActivationEmailSendEvent(userId, email) :: Nil
      }
  }
}
