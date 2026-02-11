package forex.services.oneFrame.dtos

import cats.implicits.toShow
import forex.domain.Rate

final case class OneFrameRequestDTO(
    pairs: List[Rate.Pair]
) {
  def asMultiValueQueryParams: Map[String, Seq[String]] =
    Map("pair" -> pairs.map(pair => s"${pair.from.show}${pair.to.show}"))
}

object OneFrameRequestDTO {

  def fromPair(pair: Rate.Pair): OneFrameRequestDTO =
    OneFrameRequestDTO(List(pair))

  def fromPairs(pairs: List[Rate.Pair]): OneFrameRequestDTO =
    OneFrameRequestDTO(pairs)
}
