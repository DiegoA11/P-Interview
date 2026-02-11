package forex.domain

import cats.Show
import forex.domain.Currency._
import forex.domain.Generators._
import forex.domain.errors.DomainError.InvalidCurrency
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object CurrencySpec extends SimpleIOSuite with Checkers {

  private val knownMappings: List[(String, Currency)] = List(
    "AUD" -> AUD,
    "CAD" -> CAD,
    "CHF" -> CHF,
    "EUR" -> EUR,
    "GBP" -> GBP,
    "NZD" -> NZD,
    "JPY" -> JPY,
    "SGD" -> SGD,
    "USD" -> USD,
  )

  pureTest("fromString should parse all known ISO currency codes") {
    forEach(knownMappings) {
      case (code, expected) =>
        expect(Currency.fromString(code) == Right(expected))
    }
  }

  pureTest("fromString should be case-insensitive") {
    forEach(knownMappings) {
      case (code, expected) =>
        expect.all(
          Currency.fromString(code.toLowerCase) == Right(expected),
          Currency.fromString(code.capitalize) == Right(expected)
        )
    }
  }

  pureTest("Show should produce the known code") {
    forEach(knownMappings) {
      case (code, currency) =>
        expect(Show[Currency].show(currency) == code)
    }
  }

  pureTest("fromString should return InvalidCurrency for unknown codes") {
    val invalid = List("XXX", "EURO", "", "123")
    forEach(invalid) { code =>
      Currency
        .fromString(code)
        .fold(
          {
            case InvalidCurrency(invalidCurrency) => expect(invalidCurrency == code.toUpperCase)
            case _                                => failure(s"Expected InvalidCurrency for '$code'")
          },
          _ => failure(s"Expected Left for '$code'")
        )
    }
  }

  test("fromString roundtrips with Show for any valid currency") {
    forall(currencyGen) { currency =>
      val shown  = Show[Currency].show(currency)
      val parsed = Currency.fromString(shown)
      expect(parsed == Right(currency))
    }
  }

  test("fromString always returns Left for random non-currency strings") {
    forall(invalidCurrencyCodeGen) { invalid =>
      expect(Currency.fromString(invalid).isLeft)
    }
  }
}
