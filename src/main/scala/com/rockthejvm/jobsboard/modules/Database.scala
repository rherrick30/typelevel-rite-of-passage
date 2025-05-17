package com.rockthejvm.jobsboard.modules

import cats.effect.*
import cats.effect.kernel.MonadCancelThrow
import cats.implicits.*
import com.rockthejvm.jobsboard.config.*
import doobie.ExecutionContexts
import doobie.hikari.HikariTransactor

object Database {

  def makePostgresResource[F[_] : Async] (config: PostgresConfig) : Resource[F, HikariTransactor[F]] = for {
    ce <- ExecutionContexts.fixedThreadPool(config.nthreads)
    xa <- HikariTransactor.newHikariTransactor[F](
      "org.postgresql.Driver",
      config.url,
      config.user,
      config.password,
      ce
    )
  } yield xa
  
}
