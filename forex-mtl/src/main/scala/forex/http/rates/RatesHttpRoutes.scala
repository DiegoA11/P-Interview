package forex.http
package rates

import cats.effect.Sync
import cats.syntax.all._
import forex.programs.RatesProgram
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(fromParam) +& ToQueryParam(toParam) =>
      (fromParam, toParam).tupled.fold(
        errors => BadRequest(errors.map(_.message).toList.mkString(", ")), {
          case (from, to) =>
            val apiRequest = GetApiRequest(from = from, to = to)

            apiRequest.toProgramRequest match {
              case Left(msg) =>
                BadRequest(msg)

              case Right(programRequest) =>
                rates.get(programRequest).flatMap {
                  case Right(rate) => Ok(rate.asGetApiResponse)
                  case Left(error) => BadGateway(error.getMessage)
                }
            }
        }
      )
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
