package forex.services.oneFrame.interpreters

import cats.effect.IO
import cats.syntax.all._
import forex.config.OneFrameClientConfig
import forex.domain.{ Currency, Rate }
import forex.domain.AppError
import forex.utils.NoOpLogger
import org.http4s.Method.GET
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.{ HttpApp, Request, Response, Status }
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.Logger
import weaver.SimpleIOSuite

import java.time.OffsetDateTime

object OneFrameLiveClientSpec extends SimpleIOSuite {

  private implicit val noopLogger: Logger[IO] = NoOpLogger[IO]

  private val token = "<TOKEN_PLACEHOLDER>"

  private val config =
    OneFrameClientConfig(
      host = "test-host",
      port = 1234,
      token = token
    )

  private def buildMockClient(app: HttpApp[IO]): OneFrameLiveClient[IO] =
    new OneFrameLiveClient[IO](Client.fromHttpApp(app), config)

  private val usdJpy: Rate.Pair = Rate.Pair(Currency.USD, Currency.JPY)
  private val eurUsd: Rate.Pair = Rate.Pair(Currency.EUR, Currency.USD)

  test("sends GET /rates with repeated pair params and token header") {
    val app: HttpApp[IO] = HttpApp { req: Request[IO] =>
      val assertions =
        expect.all(
          req.method == GET,
          req.uri.path.renderString == "/rates",
          req.headers.get(ci"token").exists(_.head.value == token),
          req.multiParams.getOrElse("pair", Nil).toList == List("USDJPY", "EURUSD")
        )

      val body =
        s"""[
           |  {"from":"USD","to":"JPY","bid":0.61,"ask":0.82,"price":0.71,"time_stamp":"2019-01-01T00:00:00Z"},
           |  {"from":"EUR","to":"USD","bid":1.10,"ask":1.20,"price":1.15,"time_stamp":"2019-01-01T00:00:00Z"}
           |]""".stripMargin

      assertions.run.fold(
        fail => Response[IO](Status.InternalServerError).withEntity(fail.toString).pure[IO],
        _ => Ok(body)
      )
    }

    val client = buildMockClient(app)

    client.get(List(usdJpy, eurUsd)).map { result =>
      expect.all(result.isRight, result.toOption.exists(_.size == 2))
    }
  }

  test("returns Left when OneFrame responds with non-2xx status (includes body text)") {
    val app: HttpApp[IO] = HttpApp { _: Request[IO] =>
      Response[IO](Status.TooManyRequests).withEntity("quota exceeded").pure[IO]
    }

    val client = buildMockClient(app)

    client.get(List(usdJpy)).map {
      case Left(err) =>
        expect.all(err.message.contains("HTTP 429"), err.message.toLowerCase.contains("quota exceeded"))
      case Right(_) =>
        failure("Expected Left(ServiceError), got Right")
    }
  }

  test("returns Left when OneFrame returns an empty list and pairs are non empty") {
    val app: HttpApp[IO] = HttpApp { _: Request[IO] =>
      Ok("[]")
    }

    val client = buildMockClient(app)

    client.get(List(usdJpy)).map {
      _.fold(
        error => expect(error.isInstanceOf[AppError.ServiceError]),
        response => failure(s"Expected Left, got Right($response)")
      )
    }
  }

  test("returns Left when OneFrame returns invalid JSON") {
    val app: HttpApp[IO] = HttpApp { _: Request[IO] =>
      Ok("""{"not":"a list"}""")
    }

    val client = buildMockClient(app)

    client.get(List(usdJpy)).map {
      _.fold(
        error => expect(error.message.toLowerCase.contains("oneframe http error")),
        response => failure(s"Expected Left, got Right($response)")
      )
    }
  }

  test("returns Left when OneFrame returns an invalid currency code in payload") {
    val app: HttpApp[IO] = HttpApp { _: Request[IO] =>
      val body =
        s"""[
           |  {"from":"XXX","to":"JPY","bid":0.61,"ask":0.82,"price":0.71,"time_stamp":"${OffsetDateTime
             .now()
             .toString}"}
           |]""".stripMargin
      Ok(body)
    }

    val client = buildMockClient(app)

    client.get(List(usdJpy)).map {
      _.fold(
        error => expect(error.message.toLowerCase.contains("invalid response")),
        response => failure(s"Expected Left, got Right($response)")
      )
    }
  }
}
