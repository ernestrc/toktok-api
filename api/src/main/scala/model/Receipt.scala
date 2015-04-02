package model

import com.novus.salat.{Grater, _}
import com.novus.salat.global._

/**
 * Basic reporting class.
 *
 * @param success Quick check if task was successful
 * @param errors List of errors, by default empty.
 */
case class Receipt(success: Boolean, updated: SID[_] = SID.empty[AnyRef],
                   message: String = "", errors: List[String] = List.empty)

object Receipt {

  def error(e: Throwable, message: String = ""): Receipt =
    Receipt(success = false, message = message, errors = List(e.getMessage))

  def compileResults(s: Seq[Receipt]): Receipt =
    s.foldLeft(Receipt(success = true)) { (globalRes, taskRes) =>
      Receipt(globalRes.success && taskRes.success, errors = taskRes.errors ::: globalRes.errors,
        message = globalRes.message + "-" + taskRes.message)
    }

  implicit val receiptGrater: Grater[Receipt] = grater[Receipt]

}
