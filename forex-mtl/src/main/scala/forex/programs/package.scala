package forex

package object programs {
  type RatesProgram[F[_]] = rates.RatesProgramAlgebra[F]
  final val RatesProgram = rates.RatesProgram
}
