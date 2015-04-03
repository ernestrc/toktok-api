package service

import org.bson.types.ObjectId

trait Command {

  val uuid: Option[ObjectId] = None

}
