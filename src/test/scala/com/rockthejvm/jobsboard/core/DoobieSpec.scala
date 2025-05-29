package com.rockthejvm.jobsboard.core

import cats.effect.*
import doobie.*
import doobie.implicits.*
import doobie.util.*
import doobie.hikari.HikariTransactor
import org.testcontainers.containers.PostgreSQLContainer

trait DoobieSpec {

  // simulate a database
  // use docker containers
  // testContainers

  val initScript: String

  val postgres: Resource[IO, PostgreSQLContainer[Nothing]] = {
    val acquire = IO {
      val container: PostgreSQLContainer[Nothing] = new PostgreSQLContainer("postgres").withInitScript(initScript)
      container.start()
      container
    }
    val release = (container: PostgreSQLContainer[Nothing]) => IO(container.stop())
    Resource.make(acquire)(release)
  }

  // set up a Postgres transactor
  val transactor : Resource[IO, Transactor[IO]] = for {
    db <- postgres
    ce <- ExecutionContexts.fixedThreadPool[IO](1)
    xa <- HikariTransactor.newHikariTransactor[IO] (
      "org.postgresql.Driver",
      db.getJdbcUrl(),
      db.getUsername(),
      db.getPassword(),
      ce
    )
  } yield xa


}
