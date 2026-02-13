package forex.services.oneFrame

import forex.domain.{ AppError, Rate }

trait OneFrameClientAlgebra[F[_]] {
  def get(pairs: List[Rate.Pair]): F[Either[AppError, List[Rate]]]
}
