package dal

import com.mongodb.casbah.Imports._
import com.novus.salat._
import service.Event
import unstable.macros.TypeHint
import utils.Implicits._

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import scala.util.Try

/**
 * Created by ernest on 4/1/15.
 */
class MongoSource[T <: Event : ClassTag](db: MongoDB,
                              serializers: PartialFunction[TypeHint, Grater[_ <: T]]) {

  val collectionT = db(implicitly[ClassTag[T]].runtimeClass.getSimpleName)

  def findByEntityId(id: ObjectId)(implicit ctx: ExecutionContext): Try[List[_ <: T]] = Try {
    collectionT.find(MongoDBObject("_id" → id)).toList.map { mongoObject ⇒
      serializers(mongoObject.as[String]("typeHint").toHint).asObject(mongoObject)
    }
  }

  def subscribe[E <: T]: Try[Int] = ???

  def save[E <: T](event: E): Try[ObjectId] = Try {
    val obj = serializers(event.typeHint).asInstanceOf[Grater[E]].asDBObject(event)
    collectionT.insert(obj)
    obj._id.get
  }

  def findAllAnchors: List[DBObject] = {
    collectionT.find("anchor" $exists true).toList
  }

}