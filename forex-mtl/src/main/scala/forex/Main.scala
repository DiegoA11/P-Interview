package forex

import scala.concurrent.ExecutionContext
import cats.effect._
import forex.config._
import forex.services.cache.interpreters.RatesRefCache
import forex.services.oneFrame.interpreters.OneFrameLiveClient
import fs2.Stream
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer] {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      client <- BlazeClientBuilder[F](ec).stream
      oneFrameClient = OneFrameLiveClient[F](client, config.oneFrame)
      ratesCache <- Stream.eval(RatesRefCache[F](oneFrameClient, config.cache))
      module = new Module[F](ratesCache, config)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}
