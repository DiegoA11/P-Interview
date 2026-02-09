package forex.programs.rates

import forex.services.errors.ServiceError

object errors {

  sealed trait RatesProgramError extends Exception
  object RatesProgramError {
    final case class RateLookupFailed(msg: String) extends RatesProgramError
  }

  def toProgramError(error: ServiceError): RatesProgramError = error match {
    case ServiceError.OneFrameLookupFailed(msg) => RatesProgramError.RateLookupFailed(msg)
  }
}
