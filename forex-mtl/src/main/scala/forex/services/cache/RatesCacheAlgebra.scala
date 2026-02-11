package forex.services.cache

import forex.domain.Rate
import forex.services.errors.ServiceError

trait RatesCacheAlgebra[F[_]] {
  def get(pair: Rate.Pair): F[Either[ServiceError, Rate]]
}
