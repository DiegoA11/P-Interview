package forex.services.oneFrame.interpreters

import cats.Applicative
import cats.syntax.all._
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.errors._
import forex.services.oneFrame.OneFrameClientAlgebra

class OneFrameClientDummy[F[_]: Applicative] extends OneFrameClientAlgebra[F] {

  override def get(pairs: List[Rate.Pair]): F[Either[ServiceError, List[Rate]]] =
    pairs
      .traverse { pair =>
        Price(100)
          .map(mockPrice => Rate(pair, mockPrice, mockPrice, mockPrice, Timestamp.now))
          .leftMap(toServiceError)
      }
      .pure[F]
}
