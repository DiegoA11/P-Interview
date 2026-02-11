package forex.http.rates

import cats.effect.IO
import cats.syntax.all._
import forex.domain._
import forex.domain.Generators._
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors.RatesProgramError
import forex.programs.rates.errors.RatesProgramError.RateLookupFailed
import forex.programs.rates.RatesProgramAlgebra
import org.http4s._
import org.http4s.implicits._
import weaver.SimpleIOSuite

object RatesHttpRoutesSpec extends SimpleIOSuite {

  private def stubProgram(
      result: Either[RatesProgramError, Rate]
  ): RatesProgramAlgebra[IO] = (_: GetRatesRequest) => IO.pure(result)

  private def buildRequest(from: String, to: String): Request[IO] =
    Request[IO](
      method = Method.GET,
      uri = uri"/rates".withQueryParam("from", from).withQueryParam("to", to)
    )

  private def runRequest(
      program: RatesProgramAlgebra[IO],
      request: Request[IO]
  ): IO[Response[IO]] =
    new RatesHttpRoutes[IO](program).routes.orNotFound.run(request)

  test("returns 200 with valid rate response for valid currency pair") {
    val pair    = Rate.Pair(Currency.USD, Currency.JPY)
    val rate    = freshRateForPair(pair)
    val program = stubProgram(Right(rate))

    for {
      response <- runRequest(program, buildRequest("USD", "JPY"))
      body <- response.as[String]
    } yield {
      expect.all(response.status == Status.Ok, body.contains("USD"), body.contains("JPY"))
    }
  }

  test("returns 502 when the program returns an error") {
    val program = stubProgram(Left(RateLookupFailed("Cache is stale")))

    for {
      response <- runRequest(program, buildRequest("USD", "JPY"))
      body <- response.as[String]
    } yield {
      expect.all(response.status == Status.BadGateway, body.contains("Cache is stale"))
    }
  }

  test("returns 400 for invalid 'from' currency") {
    val program = stubProgram(freshRateForPair(Rate.Pair(Currency.USD, Currency.JPY)).asRight)

    for {
      response <- runRequest(program, buildRequest("XXX", "JPY"))
    } yield expect(response.status == Status.BadRequest)
  }

  test("returns 400 for invalid 'to' currency") {
    val program = stubProgram(freshRateForPair(Rate.Pair(Currency.USD, Currency.JPY)).asRight)

    for {
      response <- runRequest(program, buildRequest("USD", "INVALID"))
    } yield expect(response.status == Status.BadRequest)
  }

  test("returns 400 when both currencies are invalid") {
    val program = stubProgram(freshRateForPair(Rate.Pair(Currency.USD, Currency.JPY)).asRight)

    for {
      response <- runRequest(program, buildRequest("FOO", "BAR"))
    } yield expect(response.status == Status.BadRequest)
  }

  test("returns 404 when query params are missing entirely") {
    val program = stubProgram(freshRateForPair(Rate.Pair(Currency.USD, Currency.JPY)).asRight)
    val request = Request[IO](method = Method.GET, uri = uri"/rates")

    for {
      response <- runRequest(program, request)
    } yield expect(response.status == Status.NotFound)
  }

  test("currency codes are case-insensitive in query params") {
    val pair    = Rate.Pair(Currency.USD, Currency.JPY)
    val rate    = freshRateForPair(pair)
    val program = stubProgram(Right(rate))

    for {
      response <- runRequest(program, buildRequest("usd", "jpy"))
    } yield expect(response.status == Status.Ok)
  }
}
