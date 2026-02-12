package forex.programs.rates

import cats.Applicative
import cats.syntax.all._
import errors._
import forex.domain._
import forex.services.RatesCacheService

class RatesProgram[F[_]: Applicative](
    ratesCache: RatesCacheService[F]
) extends RatesProgramAlgebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[Either[RatesProgramError, Rate]] =
    if (request.from == request.to) {
      RatesProgramError
        .RateLookupFailed(s"Cannot get rate for identical currencies: ${request.from}")
        .asLeft[Rate]
        .leftWiden[RatesProgramError]
        .pure[F]

    } else
      ratesCache
        .get(Rate.Pair(request.from, request.to))
        .map(_.leftMap(toProgramError))

}

object RatesProgram {

  def apply[F[_]: Applicative](
      ratesCache: RatesCacheService[F]
  ): RatesProgramAlgebra[F] = new RatesProgram[F](ratesCache)

}
