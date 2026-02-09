package forex.http.rates

import cats.syntax.all._
import forex.domain.Currency
import org.http4s.{ ParseFailure, QueryParamDecoder }
import org.http4s.dsl.impl.ValidatingQueryParamDecoderMatcher

object QueryParams {

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] = {
    QueryParamDecoder[String].emap { maybeCurrency =>
      Currency.fromString(maybeCurrency).leftMap { error =>
        ParseFailure("Invalid currency", error.message)
      }
    }
  }

  object FromQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("to")

}
