package forex.http.rates

import org.http4s.dsl.impl.ValidatingQueryParamDecoderMatcher

object QueryParams {

  object FromQueryParam extends ValidatingQueryParamDecoderMatcher[String]("from")
  object ToQueryParam extends ValidatingQueryParamDecoderMatcher[String]("to")

}
