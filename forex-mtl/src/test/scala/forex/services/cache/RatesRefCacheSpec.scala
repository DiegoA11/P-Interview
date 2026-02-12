package forex.services.cache

import cats.effect.IO
import cats.effect.concurrent.Ref
import forex.config.CacheConfig
import forex.domain._
import forex.domain.Generators._
import forex.services.cache.interpreters.RatesRefCache
import forex.services.errors.ServiceError
import forex.services.errors.ServiceError.OneFrameLookupFailed
import forex.services.oneFrame.OneFrameClientAlgebra
import forex.utils.NoOpLogger
import org.typelevel.log4cats.Logger
import weaver.SimpleIOSuite

import java.time.OffsetDateTime
import scala.concurrent.duration._

object RatesRefCacheSpec extends SimpleIOSuite {

  private implicit val logger: Logger[IO] = NoOpLogger[IO]

  private val defaultConfig = CacheConfig(
    refreshInterval = 5.minutes,
    maxAge = 5.minutes
  )

  private def stubClient(
      result: Either[ServiceError, List[Rate]]
  ): OneFrameClientAlgebra[IO] = (_: List[Rate.Pair]) => IO.pure(result)

  private def countingClient(
      result: Either[ServiceError, List[Rate]],
      counter: Ref[IO, Int]
  ): OneFrameClientAlgebra[IO] = (_: List[Rate.Pair]) => counter.update(_ + 1) *> IO.pure(result)

  private val allPairs: List[Rate.Pair] =
    for {
      from <- Currency.allCurrencies
      to <- Currency.allCurrencies
      if from != to
    } yield Rate.Pair(from, to)

  private def freshRatesForAllPairs: List[Rate] =
    allPairs.map(freshRateForPair)

  private def buildCache(
      client: OneFrameClientAlgebra[IO],
      config: CacheConfig = defaultConfig,
      initialData: Map[Rate.Pair, Rate] = Map.empty
  ): IO[RatesRefCache[IO]] =
    Ref.of[IO, Map[Rate.Pair, Rate]](initialData).map { ref =>
      new RatesRefCache[IO](ref, client, config)
    }

  test("get returns a rate when the cache is populated with a fresh rate") {
    val pair   = Rate.Pair(Currency.USD, Currency.JPY)
    val rate   = freshRateForPair(pair)
    val client = stubClient(Right(freshRatesForAllPairs))

    for {
      cache <- buildCache(client, initialData = Map(pair -> rate))
      result <- cache.get(pair)
    } yield
      result.fold(
        error => failure(s"Expected Right, got Left(${error.message})"),
        cachedRate => expect(cachedRate.pair == pair)
      )
  }

  test("get returns error when pair is not in cache") {
    val pair   = Rate.Pair(Currency.USD, Currency.JPY)
    val client = stubClient(Right(List.empty))

    for {
      cache <- buildCache(client, initialData = Map.empty)
      result <- cache.get(pair)
    } yield expect(result.isLeft)
  }

  test("get returns error when cached rate is stale") {
    val pair = Rate.Pair(Currency.USD, Currency.JPY)
    val staleRate = Rate(
      pair,
      Price(1.0).toOption.get,
      Price(1.1).toOption.get,
      Price(1.05).toOption.get,
      Timestamp(OffsetDateTime.now().minusMinutes(10))
    )
    val client = stubClient(Right(List.empty))
    val config = defaultConfig.copy(maxAge = 5.minutes)

    for {
      cache <- buildCache(client, config, initialData = Map(pair -> staleRate))
      result <- cache.get(pair)
    } yield
      result match {
        case Left(e)  => expect(e.message.contains("stale"))
        case Right(_) => failure("Expected stale error")
      }
  }

  test("refresh populates the cache from the OneFrame client") {
    val rates  = freshRatesForAllPairs
    val client = stubClient(Right(rates))
    val pair   = rates.head.pair

    for {
      cache <- buildCache(client)
      _ <- cache.refresh
      result <- cache.get(pair)
    } yield expect(result.isRight)
  }

  test("refresh does not clear cache when the client returns an error") {
    val pair          = Rate.Pair(Currency.USD, Currency.JPY)
    val rate          = freshRateForPair(pair)
    val failingClient = stubClient(Left(OneFrameLookupFailed("OneFrame is down")))

    for {
      cache <- buildCache(failingClient, initialData = Map(pair -> rate))
      _ <- cache.refresh // should NOT wipe out existing data
      result <- cache.get(pair)
    } yield expect(result.isRight)
  }

  test("refresh calls the OneFrame client exactly once per invocation") {
    val rates = freshRatesForAllPairs

    for {
      counter <- Ref.of[IO, Int](0)
      client = countingClient(Right(rates), counter)
      cache <- buildCache(client)
      _ <- cache.refresh
      _ <- cache.refresh
      count <- counter.get
    } yield expect(count == 2)
  }

  test("apply populates cache immediately") {
    val rates  = freshRatesForAllPairs
    val client = stubClient(Right(rates))
    val pair   = rates.head.pair

    for {
      cache <- RatesRefCache[IO](client, defaultConfig)
      result <- cache.get(pair)
    } yield expect(result.isRight)
  }
}
