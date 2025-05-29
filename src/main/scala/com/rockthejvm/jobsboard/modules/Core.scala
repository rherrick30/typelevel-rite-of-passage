package com.rockthejvm.jobsboard.modules

import cats.effect.*
import cats.implicits.*
import com.rockthejvm.jobsboard.core.*
import doobie.util.transactor.Transactor

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger


final class Core[F[_]] private(val jobs: Jobs[F])


// postgres -> jobs -> core -> httpApi -> app
object Core {


  given Logger[IO] = Slf4jLogger.getLogger[IO]
  
  def apply[F[_] : Async: Logger](xa: Transactor[F]): Resource[F, Core[F]] =
    Resource.eval(LiveJobs[F](xa))
      .map(jobs => new Core(jobs))
}