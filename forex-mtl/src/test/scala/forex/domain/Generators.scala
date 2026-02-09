package forex.domain

import org.scalacheck.{ Arbitrary, Gen }

object Generators {

  val supportedCurrencies: List[Currency] = List(
    Currency.AUD,
    Currency.CAD,
    Currency.CHF,
    Currency.EUR,
    Currency.GBP,
    Currency.NZD,
    Currency.JPY,
    Currency.SGD,
    Currency.USD
  )

  val currencyGen: Gen[Currency] =
    Gen.oneOf(supportedCurrencies)

  implicit val currencyArbitrary: Arbitrary[Currency] =
    Arbitrary(currencyGen)

  val validCurrencyCodeGen: Gen[String] =
    currencyGen.map(_.toString)

  val invalidCurrencyCodeGen: Gen[String] = {
    val validCodes = supportedCurrencies.map(_.toString).toSet
    Gen.alphaStr.suchThat(str => str.nonEmpty && !validCodes.contains(str.toUpperCase))
  }

  val currencyPairGen: Gen[(Currency, Currency)] =
    for {
      from <- currencyGen
      to <- currencyGen
    } yield (from, to)

}
