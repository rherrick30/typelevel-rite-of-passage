package com.rockthejvm.jobsboard.http.routes

import cats.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*

class HealthRoutes[F[_] : Monad] private extends Http4sDsl[F] {

  private val healthRoute: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*
    HttpRoutes.of[F] {
      case GET -> Root => Ok("All great here")
    }
  }

  val routes: HttpRoutes[F] = Router("/health" -> healthRoute)
}

object HealthRoutes {
  def apply[F[_] : Monad]: HealthRoutes[F] = new HealthRoutes[F]()
}
