package forex.services.oneFrame.mappers

import forex.services.oneFrame.dtos.OneFrameResponseDTO
import weaver.SimpleIOSuite

import java.time.OffsetDateTime

object OneFrameMapperSpec extends SimpleIOSuite {

  pureTest("toDomain should parse a valid DTO") {
    val dto = OneFrameResponseDTO(
      from = "USD",
      to = "JPY",
      bid = BigDecimal("0.61"),
      ask = BigDecimal("0.82"),
      price = BigDecimal("0.71"),
      time_stamp = OffsetDateTime.parse("2019-01-01T00:00:00Z")
    )

    val result = OneFrameMapper.toDomain(dto)

    expect(result.isRight)
  }

  pureTest("toDomain should fail on invalid currency") {
    val dto = OneFrameResponseDTO(
      from = "XXX",
      to = "JPY",
      bid = BigDecimal("0.61"),
      ask = BigDecimal("0.82"),
      price = BigDecimal("0.71"),
      time_stamp = OffsetDateTime.parse("2019-01-01T00:00:00Z")
    )

    val result = OneFrameMapper.toDomain(dto)

    expect(result.isLeft)
  }
}