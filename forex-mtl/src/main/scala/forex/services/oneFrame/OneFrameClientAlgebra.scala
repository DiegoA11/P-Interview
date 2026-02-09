package forex.services.oneFrame

import forex.domain.Rate
import forex.services.errors._

trait OneFrameClientAlgebra[F[_]] {
  def get(pair: Rate.Pair): F[Either[ServiceError, Rate]]
}
