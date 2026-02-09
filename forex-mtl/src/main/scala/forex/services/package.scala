package forex

package object services {
  type RatesService[F[_]] = oneFrame.OneFrameClientAlgebra[F]
  final val RatesServices = oneFrame.Interpreters
}
