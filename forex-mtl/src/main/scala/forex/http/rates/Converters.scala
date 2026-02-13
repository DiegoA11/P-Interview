package forex.http.rates

import cats.data.ValidatedNec
import cats.syntax.all._
import forex.domain.{ AppError, Currency, Rate }
import forex.domain.AppError.ValidationError.{ IdenticalCurrencies, InvalidCurrency }
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.{ Protocol => RatesProgramProtocol }

object Converters {
  import Protocol._

  private[rates] implicit class GetApiResponseOps(val rate: Rate) extends AnyVal {
    def asGetApiResponse: GetApiResponse =
      GetApiResponse(
        rate.pair.from.show,
        rate.pair.to.show,
        rate.bid.value,
        rate.ask.value,
        rate.price.value,
        rate.timestamp.value
      )
  }

  private[rates] implicit class GetApiRequestOps(val req: GetApiRequest) extends AnyVal {
    def toProgramRequest: ValidatedNec[AppError, GetRatesRequest] =
      (
        Currency.fromString(req.from).leftMap(_ => InvalidCurrency(req.from)).toValidatedNec,
        Currency.fromString(req.to).leftMap(_ => InvalidCurrency(req.to)).toValidatedNec
      ).mapN((from, to) => (from, to))
        .andThen {
          case (from, to) if from == to =>
            (IdenticalCurrencies(from.show)).invalidNec
          case (from, to) =>
            RatesProgramProtocol.GetRatesRequest(from, to).validNec
        }
  }

}
