package forex.http
package rates

import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder

import java.time.OffsetDateTime

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class GetApiRequest(
      from: String,
      to: String
  )

  final case class GetApiResponse(
      from: String,
      to: String,
      bid: BigDecimal,
      ask: BigDecimal,
      price: BigDecimal,
      timestamp: OffsetDateTime
  )

  implicit val offsetDateTimeEncoder: Encoder[OffsetDateTime] =
    Encoder.encodeString.contramap(_.toString)

  implicit val responseEncoder: Encoder[GetApiResponse] =
    deriveConfiguredEncoder[GetApiResponse]

}
