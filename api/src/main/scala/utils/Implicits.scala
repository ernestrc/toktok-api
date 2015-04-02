package utils

import com.novus.salat.Grater
import model.SID
import org.bson.types.ObjectId
import spray.http._
import spray.httpx.marshalling.Marshaller
import spray.httpx.unmarshalling._
import unstable.macros.{InjectedTypeHint, TypeHint}

import scala.reflect.ClassTag

object Implicits {

  implicit class pimpedObjectId(_id: ObjectId){
    def toSid[T : ClassTag]:SID[T] = SID.apply[T](_id.toString)
  }

  implicit def graterUnmarshallerConverter[T <: AnyRef](grater: Grater[T]): Unmarshaller[T] =
    graterUnmarshaller(grater)

  def graterUnmarshaller[T <: AnyRef](grater: Grater[T]) =
    Unmarshaller[T](MediaTypes.`application/json`) {
      case x: HttpEntity.NonEmpty ⇒
        grater.fromJSON(x.asString(defaultCharset = HttpCharsets.`UTF-8`))
    }

  implicit def graterFromResponseUnmarshaller[T <: AnyRef](grater: Grater[T])
  : FromRequestUnmarshaller[T] = Deserializer.fromFunction2Converter{ req: HttpRequest ⇒
    grater.fromJSON(req.entity.asString)
  }

  implicit def graterMarshallerConverter[T <: AnyRef](grater: Grater[T]): Marshaller[T] =
    graterMarshaller[T](grater)

  def graterMarshaller[T <: AnyRef](grater: Grater[T], pretty: Boolean = false) =
    Marshaller.delegate[T, String](ContentTypes.`application/json`) { value ⇒
      if(!pretty) grater.toPrettyJSON(value)
      else grater.toCompactJSON(value)
    }

  implicit class StringCanHint(s:String){
    def toHint: TypeHint = InjectedTypeHint(s)
  }
}
