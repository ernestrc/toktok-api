package service

import com.novus.salat.annotations.raw.Salat
import unstable.macros.{InjectedTypeHint, TypeHint}

@Salat
trait Event {

  val typeHint: TypeHint = InjectedTypeHint(this.getClass.getCanonicalName)

}
