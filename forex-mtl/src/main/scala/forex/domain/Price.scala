package forex.domain

import cats.syntax.all._
import forex.domain.errors.DomainError
import forex.domain.errors.DomainError.InvalidPrice

sealed abstract case class Price(value: BigDecimal)

object Price {

  def apply(value: BigDecimal): Either[DomainError, Price] =
    if (value > 0) {
      new Price(value) {}.asRight
    } else InvalidPrice(value).asLeft
}
