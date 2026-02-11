package forex

package object services {
  type RatesService[F[_]]      = oneFrame.OneFrameClientAlgebra[F]
  type RatesCacheService[F[_]] = cache.RatesCacheAlgebra[F]
  final val RatesServices = oneFrame.Interpreters
}
