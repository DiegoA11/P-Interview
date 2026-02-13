package forex.http.rates

import cats.syntax.all._
import forex.domain.Generators._
import forex.domain.AppError
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
        errors => failure(s"Expected Valid, got Invalid($errors)"),
        request => expect.all(request.from == pair.from, request.to == pair.to)
      )
    }
  }

  test("toProgramRequest is case-insensitive") {
    forall(distinctCurrencyPairGen) { pair =>
      val req = GetApiRequest(pair.from.show.toLowerCase, pair.to.show.toLowerCase)
      expect(req.toProgramRequest.isValid)
    }
  }

  test("toProgramRequest fails with IdenticalCurrencies for identical currencies") {
    forall(currencyGen) { currency =>
      val req = GetApiRequest(currency.show, currency.show)
      req.toProgramRequest.fold(
        errors => {
          val err = errors.head
          expect.all(
            err.isInstanceOf[AppError.ValidationError.IdenticalCurrencies],
            err.code == "forex.identical_currencies"
          )
        },
        _ => failure("Expected Invalid for identical currencies")
      )
    }
  }

  test("toProgramRequest fails with InvalidCurrency for invalid 'from' currency") {
    forall(invalidCurrencyCodeGen) { invalid =>
      val req = GetApiRequest(invalid, "USD")
      req.toProgramRequest.fold(
        errors => expect(errors.head.isInstanceOf[AppError.ValidationError.InvalidCurrency]),
        _ => failure("Expected Invalid")
      )
    }
  }

  test("toProgramRequest fails with InvalidCurrency for invalid 'to' currency") {
    forall(invalidCurrencyCodeGen) { invalid =>
      val req = GetApiRequest("USD", invalid)
      req.toProgramRequest.fold(
        errors => expect(errors.head.isInstanceOf[AppError.ValidationError.InvalidCurrency]),
        _ => failure("Expected Invalid")
      )
    }
  }

  test("toProgramRequest accumulates errors when both currencies are invalid") {
    val bothInvalidGen = for {
      from <- invalidCurrencyCodeGen
      to <- invalidCurrencyCodeGen
    } yield (from, to)

    forall(bothInvalidGen) { case (invalidFrom, invalidTo) =>
      val req = GetApiRequest(invalidFrom, invalidTo)
      req.toProgramRequest.fold(
        errors => {
          val errorList = errors.toNonEmptyList.toList
          expect.all(
            errorList.size == 2,
            errorList.forall(_.isInstanceOf[AppError.ValidationError.InvalidCurrency])
          )
        },
        _ => failure("Expected Invalid for both currencies")
      )
    }
  }
}
