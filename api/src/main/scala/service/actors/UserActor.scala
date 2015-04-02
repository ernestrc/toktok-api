package service.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.global._
import config.GlobalConfig
import dal.MongoSource
import model.Receipt
import org.bson.types.ObjectId
import service.{AnchorEvent, Event}
import unstable.macros.Macros._
import unstable.macros.TypeHint
import utils.Implicits._

import scala.util.{Failure, Success}

sealed trait UserCommand

case class CreateUserCommand(username: String, passwordHash: String, email: String) extends UserCommand

sealed trait UserEvent extends Event

case class CreateUserEvent(@Key("_id") _id: Option[ObjectId], username: String, passwordHash: String, email: String)
  extends UserEvent with Event with AnchorEvent

object UserActor {


  val eventSerializers: PartialFunction[TypeHint, Grater[_ <: UserEvent]] = grateSealed[UserEvent]
}

class UserActor(anchor: DBObject, client: MongoClient) extends Actor with ActorLogging {

  val username: String = anchor.as[String]("username")
  var password: String = anchor.as[String]("passwordHash")
  var email: String = anchor.as[String]("passwordHash")
  var sessionId: Option[String] = None


  override def receive: Receive = {
    case _ ⇒
  }
}

class UsersGuardian extends Actor with ActorLogging {

  val client = MongoClient(GlobalConfig.mongoHost)
  val db = client(GlobalConfig.mongoDb)
  val serializers: PartialFunction[TypeHint, Grater[_ <: UserEvent]] = UserActor.eventSerializers
  val source = new MongoSource[UserEvent](db, serializers)

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.info(s"CommandSideDao$$Users is up and running in path ${self.path}")
    source.findAllAnchors.foreach { anchor ⇒
      context.actorOf(Props(classOf[UserActor], anchor, client), anchor.as[String]("_id"))
    }
  }

  def newUser(cmd: CreateUserCommand): Receipt = {
    source.save(CreateUserEvent(None, cmd.username, cmd.passwordHash, cmd.email)) match {
      case Success(id) ⇒ Receipt(success = true, id.toSid, "Successfully created user")
      case Failure(err) ⇒
        log.error(err, "There was an error when creating user")
        Receipt.error(err, "Could not create a user")
    }
  }


  override def receive: Receive = {
    case cmd: CreateUserCommand ⇒ sender() ! newUser(cmd)
  }

}