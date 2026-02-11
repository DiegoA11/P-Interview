package forex.services.oneFrame.dtos

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import java.time.OffsetDateTime

final case class OneFrameResponseDTO(
    from: String,
    to: String,
    bid: BigDecimal,
    ask: BigDecimal,
    price: BigDecimal,
    time_stamp: OffsetDateTime
)

object OneFrameResponseDTO {
  implicit val decoder: Decoder[OneFrameResponseDTO] =
    deriveDecoder[OneFrameResponseDTO]
}
