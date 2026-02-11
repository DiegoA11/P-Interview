package forex

package object services {
  type RatesCacheService[F[_]] = cache.RatesCacheAlgebra[F]
}
