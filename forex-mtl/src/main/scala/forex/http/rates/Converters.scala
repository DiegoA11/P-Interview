package forex.http.rates

import cats.syntax.all._
import forex.domain.{ Currency, Rate }
import forex.programs.rates.{ Protocol => RatesProgramProtocol }

object Converters {
  import Protocol._

  private[rates] implicit class GetApiResponseOps(val rate: Rate) extends AnyVal {
    def asGetApiResponse: GetApiResponse =
      GetApiResponse(
        from = rate.pair.from.show,
        to = rate.pair.to.show,
        bid = rate.bid.value,
        ask = rate.ask.value,
        price = rate.price.value,
        timestamp = rate.timestamp.value
      )
  }

  private[rates] implicit class GetApiRequestOps(val req: GetApiRequest) extends AnyVal {
    def toProgramRequest: Either[String, RatesProgramProtocol.GetRatesRequest] =
      (Currency.fromString(req.from), Currency.fromString(req.to))
        .mapN { (from, to) =>
          RatesProgramProtocol.GetRatesRequest(from = from, to = to)
        }
        .leftMap(_.message)
  }

}
