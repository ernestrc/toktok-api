package dal

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import com.novus.salat._
import org.slf4j.LoggerFactory
import service.Event
import unstable.macros.TypeHint
import utils.Implicits._

import scala.reflect.ClassTag
import scala.util.Try

/**
 * Created by ernest on 4/1/15.
 */
class MongoSource[T <: Event : ClassTag](db: MongoDB,
                                         serializers: PartialFunction[TypeHint, Grater[_ <: T]]) {

  private def fromHintTo[E](mongoObject: Imports.DBObject) = {
    serializers(mongoObject.as[String]("_typeHint").toHint)
      .asObject(mongoObject)
      .asInstanceOf[E]
  }

  val log = LoggerFactory.getLogger(this.getClass)

  val collectionT = db(implicitly[ClassTag[T]].runtimeClass.getSimpleName)

  def findByEntityId(id: ObjectId): List[_ <: T] = {
    collectionT.find(MongoDBObject("_id" → id)).toList.map { mongoObject ⇒
      serializers(mongoObject.as[String]("_typeHint").toHint).asObject(mongoObject)
    }
  }

  def subscribe[E <: T]: Try[Int] = ???

  def save[E <: T](event: E): Try[ObjectId] = Try {
    val obj = serializers(event.typeHint).asInstanceOf[Grater[E]].asDBObject(event)
    collectionT.insert(obj)
    obj._id.get
  }

  def findOneById[E <: T](uuid: ObjectId)(implicit tag: ClassTag[E]): Option[E] = {
    val clazz = tag.runtimeClass.getCanonicalName
    collectionT.findOne(MongoDBObject(
      "_typeHint" → clazz, "uuid" → uuid
    )).map(fromHintTo[E])
  }

  def findAllEventsOfType[E <: T](implicit tag: ClassTag[E]): List[E] = {
    val clazz = tag.runtimeClass.getCanonicalName
    log.debug(s"Fetching all events of type $clazz")
    collectionT.find("_typeHint" $eq clazz).toList.map(fromHintTo[E])
  }

}