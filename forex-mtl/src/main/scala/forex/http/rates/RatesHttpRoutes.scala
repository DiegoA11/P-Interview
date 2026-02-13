package forex.http.rates

import cats.effect.Sync
import cats.syntax.all._
import forex.domain.AppError
import forex.domain.AppError.RequestError.InvalidRequest
import forex.programs.RatesProgram
import org.http4s.{ EntityEncoder, HttpRoutes }
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private implicit val errorResponseEncoder: EntityEncoder[F, ErrorResponse] = jsonEncoderOf

  private[http] val getRatesPrefixPath = "/rates"

  import cats.data.{ NonEmptyChain, NonEmptyList }

  private def error(e: AppError): ErrorResponse =
    ErrorResponse(List(e))

  private def errorNec(nec: NonEmptyChain[AppError]): ErrorResponse =
    ErrorResponse(nec.toList)

  private def errorNel[A](nel: NonEmptyList[A])(f: A => AppError): ErrorResponse =
    ErrorResponse(nel.toList.map(f))

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(fromParam) +& ToQueryParam(toParam) =>
      (fromParam, toParam).tupled.fold(
        errors => BadRequest(errorNel(errors)(failure => InvalidRequest(failure.message))), {
          case (from, to) =>
            GetApiRequest(from, to).toProgramRequest.fold(
              errors => BadRequest(errorNec(errors)),
              programRequest =>
                rates.get(programRequest).flatMap {
                  case Right(rate)       => Ok(rate.asGetApiResponse)
                  case Left(singleError) => BadGateway(error(singleError))
              }
            )
        }
      )
  }

  val routes: HttpRoutes[F] = Router(
    getRatesPrefixPath -> httpRoutes
  )

}
