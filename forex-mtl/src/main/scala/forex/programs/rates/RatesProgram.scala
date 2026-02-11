package forex.programs.rates

import cats.Functor
import cats.data.EitherT
import errors._
import forex.domain._
import forex.services.RatesService

class RatesProgram[F[_]: Functor](
    ratesService: RatesService[F]
) extends RatesProgramAlgebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[Either[RatesProgramError, Rate]] = {
    val pair = Rate.Pair(request.from, request.to)
    EitherT(ratesService.get(List(pair)))
      .leftMap(toProgramError)
      .subflatMap { rates =>
        rates.headOption.toRight(RatesProgramError.RateLookupFailed("Rate does not exist"))
      }
      .value
  }

}

object RatesProgram {

  def apply[F[_]: Functor](
      ratesService: RatesService[F]
  ): RatesProgramAlgebra[F] = new RatesProgram[F](ratesService)

}
