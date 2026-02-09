package forex.services

object errors {

  sealed trait ServiceError
  object ServiceError {
    final case class OneFrameLookupFailed(msg: String) extends ServiceError
  }

}
