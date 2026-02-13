package forex.services.oneFrame.mappers

import cats.data.ValidatedNec
import cats.syntax.all._
import forex.domain.{ AppError, Currency, Price, Rate, Timestamp }
import forex.domain.AppError.ServiceError.RateLookupFailed
import forex.services.oneFrame.dtos.OneFrameResponseDTO

object OneFrameMapper {

  def toDomain(dto: OneFrameResponseDTO): Either[AppError, Rate] = {
    def invalid(message: String): AppError =
      RateLookupFailed(s"Invalid response from OneFrame: $message")

    def validateCurrency(raw: String, field: String): ValidatedNec[AppError, Currency] =
      Currency
        .fromString(raw)
        .leftMap(e => invalid(s"$field: ${e.message}"))
        .toValidatedNec

    def validatePrice(raw: BigDecimal, field: String): ValidatedNec[AppError, Price] =
      Price(raw)
        .leftMap(e => invalid(s"$field: ${e.message}"))
        .toValidatedNec

    val validatedFrom  = validateCurrency(dto.from, "from")
    val validatedTo    = validateCurrency(dto.to, "to")
    val validatedBid   = validatePrice(dto.bid, "bid")
    val validatedAsk   = validatePrice(dto.ask, "ask")
    val validatedPrice = validatePrice(dto.price, "price")

    val validatedRate = (validatedFrom, validatedTo, validatedBid, validatedAsk, validatedPrice).mapN {
      (from, to, bid, ask, price) =>
        Rate(
          pair = Rate.Pair(from, to),
          bid = bid,
          ask = ask,
          price = price,
          timestamp = Timestamp(dto.time_stamp)
        )
    }

    validatedRate.leftMap { nec =>
      val combined = nec.toNonEmptyList.toList.map(_.message).mkString("; ")
      RateLookupFailed(combined)
    }.toEither
  }

}
