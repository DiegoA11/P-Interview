package forex.domain

import org.scalacheck.{ Arbitrary, Gen }

import java.time.OffsetDateTime

object Generators {

  val supportedCurrencies: List[Currency] = Currency.allCurrencies

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

  val distinctCurrencyPairGen: Gen[Rate.Pair] =
    for {
      from <- currencyGen
      to <- Gen.oneOf(supportedCurrencies.filterNot(_ == from))
    } yield Rate.Pair(from, to)

  val priceGen: Gen[Price] =
    Gen.choose(0.01, 10000.0).map(v => Price(v).toOption.get)

  val timestampGen: Gen[Timestamp] =
    Gen.choose(-3600L, 3600L).map { offsetSeconds =>
      Timestamp(OffsetDateTime.now().plusSeconds(offsetSeconds))
    }

  def freshTimestamp: Timestamp =
    Timestamp(OffsetDateTime.now())

  val rateGen: Gen[Rate] =
    for {
      pair <- distinctCurrencyPairGen
      bid <- priceGen
      ask <- priceGen
      price <- priceGen
      ts <- timestampGen
    } yield Rate(pair, bid, ask, price, ts)

  def freshRateForPair(pair: Rate.Pair): Rate =
    Rate(
      pair,
      Price(1.0).toOption.get,
      Price(1.1).toOption.get,
      Price(1.05).toOption.get,
      freshTimestamp
    )
}
