package forex.domain

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
}
