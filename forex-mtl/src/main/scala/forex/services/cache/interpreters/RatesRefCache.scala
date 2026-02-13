package forex.services.cache.interpreters

import cats.effect.concurrent.Ref
import cats.effect.{ Clock, Concurrent, Fiber, Resource, Timer }
import cats.syntax.all._
import forex.config.CacheConfig
import forex.domain.{ Currency, Rate }
import forex.domain.AppError
import forex.domain.AppError.ServiceError.RateLookupFailed
import forex.services.cache.RatesCacheAlgebra
import forex.services.oneFrame.OneFrameClientAlgebra
import org.typelevel.log4cats.Logger

import java.time.{ Duration, Instant, OffsetDateTime, ZoneOffset }
import java.util.concurrent.TimeUnit

class RatesRefCache[F[_]: Concurrent: Timer: Logger] private[cache] (
    ref: Ref[F, Map[Rate.Pair, Rate]],
    oneFrameClient: OneFrameClientAlgebra[F],
    config: CacheConfig
) extends RatesCacheAlgebra[F] {

  override def get(pair: Rate.Pair): F[Either[AppError, Rate]] =
    for {
      now <- Clock[F].realTime(TimeUnit.MILLISECONDS).map(ms => Instant.ofEpochMilli(ms).atOffset(ZoneOffset.UTC))
      cache <- ref.get
    } yield
      cache
        .get(pair)
        .fold[Either[AppError, Rate]](
          RateLookupFailed(s"No cached rate for ${pair.from}${pair.to}").asLeft
        ) { rate =>
          if (isStale(rate, now))
            RateLookupFailed(s"Cached rate for ${pair.from}${pair.to} is stale").asLeft
          else
            rate.asRight
        }

  private def isStale(rate: Rate, now: OffsetDateTime): Boolean = {
    val age = Duration.between(rate.timestamp.value, now)
    age.toMillis > config.maxAge.toMillis
  }

  private[cache] def refresh: F[Unit] =
    oneFrameClient
      .get(allPairs)
      .flatMap {
        case Right(rates) =>
          val updated = rates.map(r => r.pair -> r).toMap
          ref.set(updated) *>
            Logger[F].info(s"Cache refreshed successfully with ${rates.size} rates")

        case Left(error) =>
          Logger[F].error(s"Cache refresh failed: ${error.message}")
      }
      .handleErrorWith { throwable =>
        Logger[F].error(s"Unexpected error during cache refresh: ${throwable.getMessage}")
      }

  private[cache] def startBackgroundRefresh: F[Fiber[F, Unit]] = {
    val loop: F[Unit] =
      (Timer[F].sleep(config.refreshInterval) *> refresh.handleErrorWith { throwable =>
        Logger[F].error(s"Background refresh failed: ${throwable.getMessage}")
      }).foreverM[Unit]

    Concurrent[F].start(loop)
  }

  private val allPairs: List[Rate.Pair] =
    for {
      from <- Currency.allCurrencies
      to <- Currency.allCurrencies
      if from != to
    } yield Rate.Pair(from, to)
}

object RatesRefCache {

  def apply[F[_]: Concurrent: Timer: Logger](
      oneFrameClient: OneFrameClientAlgebra[F],
      config: CacheConfig
  ): Resource[F, RatesCacheAlgebra[F]] =
    Resource
      .make(
        for {
          ref <- Ref.of[F, Map[Rate.Pair, Rate]](Map.empty)
          cache = new RatesRefCache[F](ref, oneFrameClient, config)
          _ <- cache.refresh.handleErrorWith { throwable =>
                Logger[F].warn(s"Initial cache refresh failed, starting with empty cache: ${throwable.getMessage}")
              }
          fiber <- cache.startBackgroundRefresh
        } yield (cache, fiber)
      ) {
        case (_, fiber) =>
          Logger[F].info("Cancelling background cache refresh") *> fiber.cancel
      }
      .map(_._1)
}
