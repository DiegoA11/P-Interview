package forex.services

import forex.domain.errors.DomainError
import forex.domain.errors.DomainError.{ InvalidCurrency, InvalidPrice }
import forex.services.errors.ServiceError.OneFrameLookupFailed

object errors {

  sealed trait ServiceError
  object ServiceError {
    final case class OneFrameLookupFailed(msg: String) extends ServiceError
  }

  def toServiceError(error: DomainError): ServiceError = error match {
    case invalidCurrency: InvalidCurrency => OneFrameLookupFailed(invalidCurrency.message)
    case invalidPrice: InvalidPrice       => OneFrameLookupFailed(invalidPrice.message)
  }

}
