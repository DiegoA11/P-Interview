package forex.programs.rates

import cats.Functor
import cats.syntax.all._
import errors._
import forex.domain._
import forex.services.RatesCacheService

class RatesProgram[F[_]: Functor](
    ratesCache: RatesCacheService[F]
) extends RatesProgramAlgebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[Either[RatesProgramError, Rate]] =
    ratesCache
      .get(Rate.Pair(request.from, request.to))
      .map(_.leftMap(toProgramError))

}

object RatesProgram {

  def apply[F[_]: Functor](
      ratesCache: RatesCacheService[F]
  ): RatesProgramAlgebra[F] = new RatesProgram[F](ratesCache)

}
