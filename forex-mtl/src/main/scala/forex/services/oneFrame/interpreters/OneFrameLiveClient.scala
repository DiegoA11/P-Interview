package forex.services.oneFrame.interpreters

import cats.effect.Sync
import cats.syntax.all._
import forex.config.OneFrameClientConfig
import forex.domain.Rate
import forex.services.errors.ServiceError
import forex.services.oneFrame.OneFrameClientAlgebra
import forex.services.oneFrame.dtos.{ OneFrameRequestDTO, OneFrameResponseDTO }
import forex.services.oneFrame.mappers.OneFrameMapper
import org.http4s.Method.GET
import org.http4s.Uri.{ Authority, Path, RegName, Scheme }
import org.http4s.circe.jsonOf
import org.http4s.{ EntityDecoder, Header, Headers, Request, Uri }
import org.http4s.client.Client
import org.typelevel.ci.CIStringSyntax

final case class OneFrameLiveClient[F[_]: Sync](
    client: Client[F],
    config: OneFrameClientConfig
) extends OneFrameClientAlgebra[F] {

  private implicit val listDecoder: EntityDecoder[F, List[OneFrameResponseDTO]] =
    jsonOf[F, List[OneFrameResponseDTO]]

  private val baseRatesUri: Uri =
    Uri(
      scheme = Scheme.http.some,
      authority = Authority(host = RegName(config.host), port = config.port.some).some,
      path = Path.empty / "rates"
    )

  private val authHeaders: Headers =
    Headers(Header.Raw(ci"token", config.token))

  override def get(pairs: List[Rate.Pair]): F[Either[ServiceError, List[Rate]]] = {
    val requestDto = OneFrameRequestDTO.fromPairs(pairs)
    val uri        = baseRatesUri.withMultiValueQueryParams(requestDto.asMultiValueQueryParams)

    val request = Request[F](
      method = GET,
      uri = uri,
      headers = authHeaders
    )

    client
      .run(request)
      .use { response =>
        val serviceResponse: F[Either[ServiceError, List[Rate]]] =
          if (response.status.isSuccess) {
            response.as[List[OneFrameResponseDTO]].map { dtos =>
              dtos.traverse(OneFrameMapper.toDomain)
            }
          } else {
            response.bodyText.compile.string.map { body =>
              ServiceError
                .OneFrameLookupFailed(
                  s"OneFrame returned HTTP ${response.status.code} ${response.status.reason}. Body: $body"
                )
                .asLeft[List[Rate]]
            }
          }
        serviceResponse
      }
      .handleError { error =>
        ServiceError.OneFrameLookupFailed(s"OneFrame HTTP error: ${error.getMessage}").asLeft
      }
  }
}
