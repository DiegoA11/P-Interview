package forex.http.rates

import cats.syntax.all._
import forex.domain.Generators._
import forex.http.rates.Converters._
import forex.http.rates.Protocol.GetApiRequest
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object ConvertersSpec extends SimpleIOSuite with Checkers {

  test("asGetApiResponse maps all Rate fields correctly") {
    forall(rateGen) { rate =>
      val response = rate.asGetApiResponse
      expect.all(
        response.from == rate.pair.from.show,
        response.to == rate.pair.to.show,
        response.bid == rate.bid.value,
        response.ask == rate.ask.value,
        response.price == rate.price.value,
        response.timestamp == rate.timestamp.value
      )
    }
  }

  test("toProgramRequest succeeds for valid currency codes") {
    forall(distinctCurrencyPairGen) { pair =>
      val req = GetApiRequest(pair.from.show, pair.to.show)
      req.toProgramRequest.fold(
        message => failure(s"Expected Right, got Left($message)"),
        request => expect.all(request.from == pair.from, request.to == pair.to)
      )
    }
  }

  test("toProgramRequest is case-insensitive") {
    forall(distinctCurrencyPairGen) { pair =>
      val req = GetApiRequest(pair.from.show.toLowerCase, pair.to.show.toLowerCase)
      expect(req.toProgramRequest.isRight)
    }
  }

  test("toProgramRequest fails for invalid 'from' currency") {
    forall(invalidCurrencyCodeGen) { invalid =>
      val req = GetApiRequest(invalid, "USD")
      expect(req.toProgramRequest.isLeft)
    }
  }

  test("toProgramRequest fails for invalid 'to' currency") {
    forall(invalidCurrencyCodeGen) { invalid =>
      val req = GetApiRequest("USD", invalid)
      expect(req.toProgramRequest.isLeft)
    }
  }
}
