package model

import scala.reflect.ClassTag

/**
 * Typed string ID.
 *
 * It is favoured over a simple string id so that we catch errors at compile time.
 * This class extends AnyVal thus it will be replaced for a simple long by the compiler.
 *
 * @see http://docs.scala-lang.org/overviews/core/value-classes.html
 */
case class SID[T](sid: String) extends AnyVal {

  def isSet: Boolean = sid != ""

  def notSet: Boolean = sid == ""

  override def toString: String = sid

}

object SID {

  def empty[T: ClassTag]: SID[T] = SID[T]("")

}