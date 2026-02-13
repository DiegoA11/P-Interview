package forex.services.oneFrame.interpreters

import cats.effect.Sync
import cats.syntax.all._
import forex.config.OneFrameClientConfig
import forex.domain.{ AppError, Rate }
import forex.domain.AppError.ServiceError.RateLookupFailed
import forex.services.oneFrame.OneFrameClientAlgebra
import forex.services.oneFrame.dtos.{ OneFrameRequestDTO, OneFrameResponseDTO }
import forex.services.oneFrame.mappers.OneFrameMapper
import org.http4s.Method.GET
import org.http4s.Uri.{ Authority, Path, RegName, Scheme }
import org.http4s.circe.jsonOf
import org.http4s.{ EntityDecoder, Header, Headers, Request, Uri }
import org.http4s.client.Client
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.Logger

final class OneFrameLiveClient[F[_]: Sync: Logger](
    client: Client[F],
    config: OneFrameClientConfig
) extends OneFrameClientAlgebra[F] {

  private implicit val listDecoder: EntityDecoder[F, List[OneFrameResponseDTO]] =
    jsonOf[F, List[OneFrameResponseDTO]]

  private val baseServiceUri: Uri =
    Uri(
      scheme = Scheme.http.some,
      authority = Authority(host = RegName(config.host), port = config.port.some).some,
      path = Path.empty
    )

  private val authHeaders: Headers =
    Headers(Header.Raw(ci"token", config.token))

  override def get(pairs: List[Rate.Pair]): F[Either[AppError, List[Rate]]] = {
    val requestDto = OneFrameRequestDTO.fromPairs(pairs)
    val uri        = baseServiceUri.withMultiValueQueryParams(requestDto.asMultiValueQueryParams)

    val request = Request[F](
      method = GET,
      uri = uri / "rates",
      headers = authHeaders
    )

    type Result = Either[AppError, List[Rate]]

    Logger[F].debug(s"Requesting ${pairs.size} pair(s) from OneFrame") *>
      client
        .run(request)
        .use { response =>
          if (response.status.isSuccess) {
            response.as[List[OneFrameResponseDTO]].flatMap { dtos =>
              if (pairs.nonEmpty && dtos.isEmpty) {
                val message = s"No rates returned for requested pairs: ${pairs.mkString(", ")}"
                Logger[F].warn(message).as[Result](RateLookupFailed(message).asLeft)
              } else {
                dtos.traverse(OneFrameMapper.toDomain) match {
                  case Right(rates) =>
                    Logger[F]
                      .debug(s"Received ${rates.size} rate(s) from OneFrame")
                      .as[Result](rates.asRight)
                  case Left(error) =>
                    Logger[F]
                      .error(s"Failed to map OneFrame response: ${error.message}")
                      .as[Result](error.asLeft)
                }
              }
            }
          } else {
            response.bodyText.compile.string.flatMap { body =>
              val message = s"OneFrame returned HTTP ${response.status.code} ${response.status.reason}. Body: $body"
              Logger[F].error(message).as[Result](RateLookupFailed(message).asLeft)
            }
          }
        }
        .handleErrorWith { error =>
          val message = s"OneFrame HTTP error: ${error.getMessage}"
          Logger[F].error(error)(message).as[Result](RateLookupFailed(message).asLeft)
        }
  }
}
