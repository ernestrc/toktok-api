package io.toktok.service

import com.novus.salat.annotations.raw.Salat
import org.bson.types.BSONTimestamp
import unstable.macros.{InjectedTypeHint, TypeHint}

@Salat
trait Event {

  val _typeHint: TypeHint = InjectedTypeHint(this.getClass.getCanonicalName)

}
