package forex.http.rates

import cats.effect.IO
import cats.syntax.all._
import forex.domain._
import forex.domain.Generators._
import forex.programs.rates.Protocol.GetRatesRequest
import forex.domain.AppError
import forex.domain.AppError.ServiceError.RateLookupFailed
import forex.programs.rates.RatesProgramAlgebra
import io.circe.parser._
import io.circe.Decoder
import org.http4s._
import org.http4s.implicits._
import weaver.SimpleIOSuite

object RatesHttpRoutesSpec extends SimpleIOSuite {

  private final case class ErrorObj(code: String, message: String)
  private implicit val errorDecoder: Decoder[ErrorObj] =
    Decoder.forProduct2("code", "message")(ErrorObj.apply)

  private def stubProgram(
      result: Either[AppError, Rate]
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

  private def parseErrorBody(body: String): List[ErrorObj] =
    parse(body).toOption
      .flatMap { json =>
        json.hcursor
          .downField("errors")
          .as[List[ErrorObj]]
          .toOption
      }
      .getOrElse(Nil)

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

  test("returns 502 with rate_lookup_failed code when the program returns an error") {
    val program = stubProgram(Left(RateLookupFailed("Cache is stale")))

    for {
      response <- runRequest(program, buildRequest("USD", "JPY"))
      body <- response.as[String]
    } yield {
      val errors = parseErrorBody(body)
      expect.all(
        response.status == Status.BadGateway,
        errors.exists(_.code == "forex.rate_lookup_failed"),
        errors.exists(_.message.contains("Cache is stale"))
      )
    }
  }

  test("returns 400 with identical_currencies code when from and to are the same") {
    val program = stubProgram(freshRateForPair(Rate.Pair(Currency.USD, Currency.JPY)).asRight)

    for {
      response <- runRequest(program, buildRequest("USD", "USD"))
      body <- response.as[String]
    } yield {
      val errors = parseErrorBody(body)
      expect.all(
        response.status == Status.BadRequest,
        errors.exists(_.code == "forex.identical_currencies")
      )
    }
  }

  test("returns 400 with invalid_currency code for invalid 'from' currency") {
    val program = stubProgram(freshRateForPair(Rate.Pair(Currency.USD, Currency.JPY)).asRight)

    for {
      response <- runRequest(program, buildRequest("XXX", "JPY"))
      body <- response.as[String]
    } yield {
      val errors = parseErrorBody(body)
      expect.all(
        response.status == Status.BadRequest,
        errors.exists(_.code == "forex.invalid_currency")
      )
    }
  }

  test("returns 400 with invalid_currency code for invalid 'to' currency") {
    val program = stubProgram(freshRateForPair(Rate.Pair(Currency.USD, Currency.JPY)).asRight)

    for {
      response <- runRequest(program, buildRequest("USD", "INVALID"))
      body <- response.as[String]
    } yield {
      val errors = parseErrorBody(body)
      expect.all(
        response.status == Status.BadRequest,
        errors.exists(_.code == "forex.invalid_currency")
      )
    }
  }

  test("returns 400 with two accumulated invalid_currency errors when both currencies are invalid") {
    val program = stubProgram(freshRateForPair(Rate.Pair(Currency.USD, Currency.JPY)).asRight)

    for {
      response <- runRequest(program, buildRequest("FOO", "BAR"))
      body <- response.as[String]
    } yield {
      val errors = parseErrorBody(body)
      expect.all(
        response.status == Status.BadRequest,
        errors.size == 2,
        errors.forall(_.code == "forex.invalid_currency")
      )
    }
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
