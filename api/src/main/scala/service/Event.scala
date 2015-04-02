package service

import org.bson.types.ObjectId
import unstable.macros.{InjectedTypeHint, TypeHint}

/**
 * Created by ernest on 4/1/15.
 */
trait Event {

  val _id: Option[ObjectId]
  val typeHint: TypeHint = InjectedTypeHint(this.getClass.getName)

}
