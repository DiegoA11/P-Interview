package forex.services.oneFrame

import cats.Applicative
import cats.effect.Sync
import forex.config.OneFrameClientConfig
import interpreters._
import org.http4s.client.Client
import org.typelevel.log4cats.Logger

object Interpreters {
  def dummy[F[_]: Applicative]: OneFrameClientAlgebra[F] = new OneFrameClientDummy[F]()
  def live[F[_]: Sync: Logger](
      client: Client[F],
      oneFrameClientConfig: OneFrameClientConfig
  ): OneFrameClientAlgebra[F] = new OneFrameLiveClient[F](client, oneFrameClientConfig)
}
