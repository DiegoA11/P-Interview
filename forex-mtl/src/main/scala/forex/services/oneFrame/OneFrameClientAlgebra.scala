package forex.services.oneFrame

import forex.domain.Rate
import forex.services.errors._

trait OneFrameClientAlgebra[F[_]] {
  def get(pairs: List[Rate.Pair]): F[Either[ServiceError, List[Rate]]]
}
