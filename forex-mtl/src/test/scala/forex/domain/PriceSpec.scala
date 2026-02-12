package forex.domain

import forex.domain.errors.DomainError.InvalidPrice
import org.scalacheck.Gen
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object PriceSpec extends SimpleIOSuite with Checkers {

  test("Price should accept any positive double") {
    forall(Gen.posNum[Double]) { value =>
      Price(value).fold(
        error => failure(s"Expected Right for positive $value, got Left($error)"),
        price => expect(price.value == BigDecimal(value))
      )
    }
  }

  test("Price should reject any non-positive double") {
    forall(Gen.choose(Double.MinValue, 0)) { value =>
      expect(Price(value).isLeft)
    }
  }

  pureTest("Price should reject zero") {
    Price(0).fold(
      {
        case InvalidPrice(value) => expect(value == BigDecimal(0))
        case other               => failure(s"Expected InvalidPrice(0), got $other")
      },
      _ => failure("Expected Left for price = 0")
    )
  }
}
