package forex.domain

import cats.data.NonEmptyChain
import io.circe.{ Encoder, Json }

sealed trait AppError extends Throwable {
  def code: String
  def message: String
  override def getMessage: String = message
}

object AppError {

  sealed trait ValidationError extends AppError
  object ValidationError {
    final case class InvalidCurrency(currency: String) extends ValidationError {
      val code: String    = "forex.invalid_currency"
      val message: String = s"Unknown currency: '$currency'"
    }

    final case class InvalidPrice(price: BigDecimal) extends ValidationError {
      val code: String    = "forex.invalid_price"
      val message: String = s"Invalid price: '$price'"
    }

    final case class IdenticalCurrencies(currency: String) extends ValidationError {
      val code: String    = "forex.identical_currencies"
      val message: String = s"Cannot get rate for identical currencies: $currency"
    }
  }

  sealed trait ServiceError extends AppError
  object ServiceError {
    final case class RateLookupFailed(detail: String) extends ServiceError {
      val code: String    = "forex.rate_lookup_failed"
      val message: String = detail
    }
  }

  sealed trait RequestError extends AppError
  object RequestError {
    final case class InvalidRequest(detail: String) extends RequestError {
      val code: String    = "forex.invalid_request"
      val message: String = detail
    }
  }

  final case class Multiple(errors: NonEmptyChain[AppError]) extends AppError {
    val code: String    = errors.head.code
    val message: String = errors.toNonEmptyList.toList.map(_.message).mkString("; ")
  }

  private val singleEncoder: Encoder[AppError] =
    Encoder.forProduct2("code", "message")(e => (e.code, e.message))

  implicit val encoder: Encoder[AppError] = Encoder.instance {
    case Multiple(errors) => Json.arr(errors.toNonEmptyList.toList.map(singleEncoder.apply): _*)
    case single           => singleEncoder(single)
  }
}
