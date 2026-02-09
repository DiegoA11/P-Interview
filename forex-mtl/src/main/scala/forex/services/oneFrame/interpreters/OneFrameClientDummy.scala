package forex.services.oneFrame.interpreters

import forex.services.oneFrame.OneFrameClientAlgebra
import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.errors._

class OneFrameClientDummy[F[_]: Applicative] extends OneFrameClientAlgebra[F] {

  override def get(pair: Rate.Pair): F[Either[ServiceError, Rate]] =
    Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[ServiceError].pure[F]

}
