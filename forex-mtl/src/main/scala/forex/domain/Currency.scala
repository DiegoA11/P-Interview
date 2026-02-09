package forex.domain

import cats.Show
import cats.syntax.all._
import forex.domain.errors.DomainError
import forex.domain.errors.DomainError.InvalidCurrency

sealed trait Currency

object Currency {
  case object AUD extends Currency
  case object CAD extends Currency
  case object CHF extends Currency
  case object EUR extends Currency
  case object GBP extends Currency
  case object NZD extends Currency
  case object JPY extends Currency
  case object SGD extends Currency
  case object USD extends Currency

  implicit val show: Show[Currency] = Show.fromToString

  def fromString(s: String): Either[DomainError, Currency] = s.toUpperCase match {
    case "AUD"   => AUD.asRight
    case "CAD"   => CAD.asRight
    case "CHF"   => CHF.asRight
    case "EUR"   => EUR.asRight
    case "GBP"   => GBP.asRight
    case "NZD"   => NZD.asRight
    case "JPY"   => JPY.asRight
    case "SGD"   => SGD.asRight
    case "USD"   => USD.asRight
    case invalid => InvalidCurrency(invalid).asLeft
  }

}
