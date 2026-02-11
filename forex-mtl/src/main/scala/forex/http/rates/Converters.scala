package forex.http.rates

import cats.syntax.all._
import forex.domain.{ Currency, Rate }
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
    def toProgramRequest: Either[String, RatesProgramProtocol.GetRatesRequest] =
      (Currency.fromString(req.from), Currency.fromString(req.to))
        .mapN { (from, to) =>
          RatesProgramProtocol.GetRatesRequest(from, to)
        }
        .leftMap(_.message)
  }

}
