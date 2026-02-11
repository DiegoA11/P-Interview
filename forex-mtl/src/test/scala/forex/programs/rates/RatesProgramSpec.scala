package forex.programs.rates

import cats.Id
import forex.domain._
import forex.domain.Generators._
import forex.programs.rates.Protocol.GetRatesRequest
import forex.services.cache.RatesCacheAlgebra
import forex.services.errors.ServiceError
import forex.services.errors.ServiceError.OneFrameLookupFailed
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object RatesProgramSpec extends SimpleIOSuite with Checkers {

  private def stubCache(result: Either[ServiceError, Rate]): RatesCacheAlgebra[Id] =
    (_: Rate.Pair) => result

  test("get returns a Rate when the cache has a fresh rate") {
    forall(distinctCurrencyPairGen) { pair =>
      val rate    = freshRateForPair(pair)
      val cache   = stubCache(Right(rate))
      val program = RatesProgram[Id](cache)

      val result = program.get(GetRatesRequest(pair.from, pair.to))

      expect(result == Right(rate))
    }
  }

  test("get returns RateLookupFailed when the cache returns an error") {
    forall(distinctCurrencyPairGen) { pair =>
      val errorMsg = s"No cached rate for ${pair.from}${pair.to}"
      val cache    = stubCache(Left(OneFrameLookupFailed(errorMsg)))
      val program  = RatesProgram[Id](cache)

      val result = program.get(GetRatesRequest(pair.from, pair.to))

      result.fold(
        error => expect(error.message == errorMsg),
        other => failure(s"Expected RateLookupFailed, got $other")
      )
    }
  }
}
