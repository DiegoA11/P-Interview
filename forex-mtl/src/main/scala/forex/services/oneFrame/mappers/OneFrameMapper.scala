package forex.services.oneFrame.mappers

import cats.data.ValidatedNec
import cats.syntax.all._
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.errors.ServiceError
import forex.services.errors.ServiceError.OneFrameLookupFailed
import forex.services.oneFrame.dtos.OneFrameResponseDTO

object OneFrameMapper {

  def toDomain(dto: OneFrameResponseDTO): Either[ServiceError, Rate] = {
    def invalid(msg: String): ServiceError =
      OneFrameLookupFailed(s"Invalid response from OneFrame: $msg")

    def validateCurrency(raw: String, field: String): ValidatedNec[ServiceError, Currency] =
      Currency
        .fromString(raw)
        .leftMap(e => invalid(s"$field: ${e.message}"))
        .toValidatedNec

    def validatePrice(raw: BigDecimal, field: String): ValidatedNec[ServiceError, Price] =
      Price(raw.doubleValue)
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
      OneFrameLookupFailed(combined)
    }.toEither
  }

}
