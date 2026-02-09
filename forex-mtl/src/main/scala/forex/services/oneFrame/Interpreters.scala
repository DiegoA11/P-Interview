package forex.services.oneFrame

import cats.Applicative
import interpreters._

object Interpreters {
  def dummy[F[_]: Applicative]: OneFrameClientAlgebra[F] = new OneFrameClientDummy[F]()
}
