package forex.services.cache

import forex.domain.{ AppError, Rate }

trait RatesCacheAlgebra[F[_]] {
  def get(pair: Rate.Pair): F[Either[AppError, Rate]]
}
