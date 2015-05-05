package io.toktok.command.users.actors

import java.util.NoSuchElementException

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor._
import akka.event.LoggingAdapter
import akka.pattern._
import akka.util.Timeout
import com.mongodb.casbah.MongoClient
import io.toktok.command.users.Exceptions.UserExistsException
import io.toktok.command.users.ServiceConfig
import io.toktok.model._
import krakken.dal.MongoSource
import krakken.io._
import krakken.model.{Command, Receipt, SID}
import krakken.utils.Implicits._
import org.bson.types.ObjectId
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

class UserCommandGuardian extends Actor with ActorLogging {

  import context.dispatcher

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(loggingEnabled = true) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Restart
    case _: DeathPactException => Restart
    case _: Exception => Restart
  }

  override def preStart(): Unit = {
    log.info(s"UsersGuardian is up and running in path ${self.path}")
    source.findAllEventsOfType[UserCreatedAnchor].foreach { anchor ⇒
      log.info(s"Booting up user actor for user ${anchor.username}")
      addUser(anchor)
      context.actorOf(Props(classOf[UserCommandActor], anchor), anchor.uuid.get.toString)
    }
    log.info(s"Users' actors: $users")
  }

  implicit val timeout: Timeout = ServiceConfig.ACTOR_TIMEOUT
  implicit val logger: LoggingAdapter = log

  val discoveryActor = context.actorOf(Props[DiscoveryActor])
  val mongoContainer: Option[Service] = Try(Await.result(discoveryActor.ask(
    DiscoveryActor.Find(ServiceConfig.dataContainer))(ServiceConfig.ACTOR_TIMEOUT)
    .mapTo[Service], ServiceConfig.ACTOR_TIMEOUT))
    .toOption
  val mongoHost: String =  mongoContainer.map(_.host.ip).getOrElse(ServiceConfig.mongoHost)
  val mongoPort: Int = mongoContainer.map(_.port).getOrElse(ServiceConfig.mongoPort)
  val dbName: String = ServiceConfig.dbName
  log.debug("{} container linked -> {}", ServiceConfig.dataContainer, mongoContainer)
  val db = MongoClient(mongoHost, mongoPort)(dbName)
  val source = new MongoSource[UserEvent](db)

  def addUser(anchor: UserCreatedAnchor): Unit = {
    users += ((anchor.uuid.get, anchor.username, anchor.email))
  }

  val users = scala.collection.mutable.Set.empty[(ObjectId, String, String)]

  def createNewUserCommand(username: String, pass: String, email: String): Future[Receipt[_]] = Future {
    if (users.exists(_._2 == username)) throw UserExistsException(username)
    else if(users.exists(_._3 == email)) throw UserExistsException(email)
    else {
      val passHash = BCrypt.hashpw(pass, BCrypt.gensalt())
      val anchor = UserCreatedAnchor(None, username, passHash, email)
      val id = Some(source.save(anchor).get.toObjectId)
      anchor.copy(uuid = id)
    }
  }.map { anchor: UserCreatedAnchor ⇒
    addUser(anchor)
    val msg = s"Successfully created user $username"
    context.actorOf(Props(classOf[UserCommandActor], anchor), anchor.uuid.get.toString)
    log.info(msg)
    Receipt(success = true, entity = anchor.uuid.get.toSid, message = msg)
  }.recover {
    case ex@UserExistsException(user) ⇒
      val msg = s"Could not create user $user because it already exists!"
      log.debug(msg)
      Receipt.error(ex, msg)
    case err: Exception ⇒
      val msg = s"Could not create user $username"
      log.error(err, msg)
      Receipt.error(err, msg)
  }.andThen {
    //auto activate white-listed
    case Success(rec) if rec.success ⇒
      val uuid = rec.entity.asInstanceOf[SID]
      if (ServiceConfig.WHITELIST_EMAIL.exists(email.matches)) {
        val selection = context.system.actorSelection(context.system / self.path.name / uuid)
        selection.resolveOne().map { c ⇒
          log.info(s"User's \'$username\' email \'$email\' is whitelisted. Activating user now!")
          c.ask(ActivateUserCommand(uuid))
        }.andThen {
          case Failure(err) ⇒ log.error("Could not activate whitelisted user!")
        }
      }
      else log.info(s"User's \'$username\' email \'$email\' not in whitelist")
  }

  def forwardToChildOrCreateNew(cmd: Command, requester: ActorRef): Future[Unit] = Future {
    val uuid = cmd.entityId
    context.stop(context.child(uuid.toString).get)
    log.warning(s"Actor of $uuid was unresponsive so it was stopped!")
    val anchor: UserCreatedAnchor = source.findOneByObjectId[UserCreatedAnchor](uuid.toObjectId).get
    val reborn = context.actorOf(Props(classOf[UserCommandActor], anchor, source), uuid)
    reborn.tell(cmd, requester)
  }.recover {
    case err: Exception ⇒ requester ! Receipt.error(err, "Unable to process command!")
  }

  def forgotPasswordCommand(cmd: ForgotPasswordCommand): Future[Receipt[_]] = Future {
    val user = users.find(_._2 == cmd.username).get
    log.debug("Found user {} in users collection of UserCommandGuardian", user)
    context.child(user._1.toString).get
  }.flatMap { userActor ⇒
    userActor.ask(cmd)(ServiceConfig.ACTOR_TIMEOUT).mapTo[Receipt[_]]
  }.recover {
    case ex: NoSuchElementException ⇒
      Receipt(success = false, entity = None, message = "Username doesn't correspond to any user")
  }

  override def receive: Receive = {
    case cmd: ForgotPasswordCommand ⇒ forgotPasswordCommand(cmd) pipeTo sender()
    case CreateUserCommand(username, pass, email) ⇒
      createNewUserCommand(username, pass, email) pipeTo sender()
    case cmd: UserCommand ⇒ forwardToChildOrCreateNew(cmd, sender())
    case anyElse ⇒ log.error(s"Oops, it looks like I shouldn't have received $anyElse")
  }

}