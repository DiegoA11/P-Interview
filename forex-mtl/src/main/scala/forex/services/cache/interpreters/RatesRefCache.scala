package forex.services.cache.interpreters

import cats.effect.concurrent.Ref
import cats.effect.{ Concurrent, Timer }
import cats.syntax.all._
import forex.config.CacheConfig
import forex.domain.{ Currency, Rate }
import forex.services.cache.RatesCacheAlgebra
import forex.services.errors.ServiceError
import forex.services.errors.ServiceError.OneFrameLookupFailed
import forex.services.oneFrame.OneFrameClientAlgebra
import org.typelevel.log4cats.Logger

import java.time.{ Duration, OffsetDateTime }

class RatesRefCache[F[_]: Concurrent: Timer: Logger] private[cache] (
    ref: Ref[F, Map[Rate.Pair, Rate]],
    oneFrameClient: OneFrameClientAlgebra[F],
    config: CacheConfig
) extends RatesCacheAlgebra[F] {

  override def get(pair: Rate.Pair): F[Either[ServiceError, Rate]] =
    ref.get.map { cache =>
      cache
        .get(pair)
        .fold[Either[ServiceError, Rate]](
          OneFrameLookupFailed(s"No cached rate for ${pair.from}${pair.to}").asLeft
        ) { rate =>
          if (isStale(rate))
            OneFrameLookupFailed(s"Cached rate for ${pair.from}${pair.to} is stale").asLeft
          else
            rate.asRight
        }
    }

  private def isStale(rate: Rate): Boolean = {
    val now = OffsetDateTime.now()
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

  private[cache] def startBackgroundRefresh: F[Unit] = {
    val loop: F[Unit] =
      (Timer[F].sleep(config.refreshInterval) *> refresh).foreverM[Unit]

    Concurrent[F].start(loop).void
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
  ): F[RatesCacheAlgebra[F]] =
    for {
      ref <- Ref.of[F, Map[Rate.Pair, Rate]](Map.empty)
      cache = new RatesRefCache[F](ref, oneFrameClient, config)
      _ <- cache.refresh
      _ <- cache.startBackgroundRefresh
    } yield cache
}
