package forex.services

import forex.domain.errors.DomainError
import forex.domain.errors.DomainError.{ InvalidCurrency, InvalidPrice }
import forex.services.errors.ServiceError.OneFrameLookupFailed

object errors {

  sealed trait ServiceError {
    def message: String
  }
  object ServiceError {
    final case class OneFrameLookupFailed(msg: String) extends ServiceError {
      override val message: String = msg
    }
  }

  def toServiceError(error: DomainError): ServiceError = error match {
    case invalidCurrency: InvalidCurrency => OneFrameLookupFailed(invalidCurrency.message)
    case invalidPrice: InvalidPrice       => OneFrameLookupFailed(invalidPrice.message)
  }

}
