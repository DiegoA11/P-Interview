package forex.domain

import cats.Show

case class Rate(
    pair: Rate.Pair,
    bid: Price,
    ask: Price,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  object Pair {
    implicit val show: Show[Pair] = Show.fromToString
  }

  implicit val show: Show[Rate] = Show.fromToString
}
