package forex.programs.rates

import forex.domain._
import forex.services.RatesCacheService

class RatesProgram[F[_]](
    ratesCache: RatesCacheService[F]
) extends RatesProgramAlgebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[Either[AppError, Rate]] =
    ratesCache.get(Rate.Pair(request.from, request.to))

}

object RatesProgram {

  def apply[F[_]](
      ratesCache: RatesCacheService[F]
  ): RatesProgramAlgebra[F] = new RatesProgram[F](ratesCache)

}
