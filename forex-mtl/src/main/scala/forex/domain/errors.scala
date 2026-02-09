package forex.domain

object errors {

  sealed trait DomainError {
    def message: String
  }

  object DomainError {
    final case class InvalidCurrency(invalidCurrency: String) extends DomainError {
      override def message: String = s"Unknown currency: '$invalidCurrency'"
    }
  }

}
