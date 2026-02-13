package forex.programs.rates

import forex.domain.{ AppError, Rate }

trait RatesProgramAlgebra[F[_]] {
  def get(request: Protocol.GetRatesRequest): F[Either[AppError, Rate]]
}
