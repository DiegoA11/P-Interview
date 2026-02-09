package forex.programs.rates

import forex.programs.rates.errors.RatesProgramError.RateLookupFailed
import forex.services.errors.ServiceError
import forex.services.errors.ServiceError.OneFrameLookupFailed

object errors {

  sealed trait RatesProgramError extends Exception {
    def message: String
    override def getMessage: String = message
  }

  object RatesProgramError {
    final case class RateLookupFailed(msg: String) extends RatesProgramError {
      override def message: String = msg
    }
  }

  def toProgramError(error: ServiceError): RatesProgramError = error match {
    case OneFrameLookupFailed(msg) => RateLookupFailed(msg)
  }
}
