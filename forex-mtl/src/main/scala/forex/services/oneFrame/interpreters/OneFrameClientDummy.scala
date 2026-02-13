package forex.services.oneFrame.interpreters

import cats.Applicative
import cats.syntax.all._
import forex.domain.{ AppError, Price, Rate, Timestamp }
import forex.services.oneFrame.OneFrameClientAlgebra

class OneFrameClientDummy[F[_]: Applicative] extends OneFrameClientAlgebra[F] {

  override def get(pairs: List[Rate.Pair]): F[Either[AppError, List[Rate]]] =
    pairs
      .traverse { pair =>
        Price(100)
          .map(mockPrice => Rate(pair, mockPrice, mockPrice, mockPrice, Timestamp.now))
      }
      .pure[F]
}
