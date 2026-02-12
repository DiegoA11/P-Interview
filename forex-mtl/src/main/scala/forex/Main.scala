package forex

import scala.concurrent.ExecutionContext
import cats.effect._
import forex.config._
import forex.services.cache.interpreters.RatesRefCache
import forex.services.oneFrame.interpreters.OneFrameLiveClient
import fs2.Stream
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer] {

  implicit val logger: Logger[F] = Slf4jLogger.getLoggerFromClass[F](classOf[Application[F]])

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      _ <- Stream.eval(Logger[F].info("Starting app"))
      client <- BlazeClientBuilder[F](ec).stream
      oneFrameClient = new OneFrameLiveClient[F](client, config.oneFrame)
      _ <- Stream.eval(Logger[F].info(s"Connected to OneFrame at ${config.oneFrame.host}:${config.oneFrame.port}"))
      ratesCache <- Stream.eval(RatesRefCache[F](oneFrameClient, config.cache))
      _ <- Stream.eval(Logger[F].info(s"Cache initialized, refresh interval: ${config.cache.refreshInterval}"))
      module = new Module[F](ratesCache, config)
      _ <- Stream.eval(Logger[F].info(s"HTTP server starting on ${config.http.host}:${config.http.port}"))
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}
