package com.rockthejvm.jobsboard.playground

import cats.effect.*
import com.rockthejvm.jobsboard.core.LiveJobs
import com.rockthejvm.jobsboard.domain.job.*
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.util.*

import scala.io.StdIn

object JobsPlayground extends IOApp.Simple {

  val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:docker",
      "docker",
      "docker", // The password
      ce
    )
  } yield xa

  val jobInfo: JobInfo = JobInfo(
    company = "Rock The JVM",
    title = "Software Engineer",
    description = "Best Job everrrrr",
    externalUrl = "www.rockthejvm.com",
    remote = true,
    location = "Romania"
  )

  override def run: IO[Unit] = postgresResource.use { xa =>
    for {
      jobs <- LiveJobs[IO](xa)
      _ <- IO(println("Ready, Next.....")) *> IO(StdIn.readLine)
      id <- jobs.create("rherrick@rockthejvm.com", jobInfo)
      _ <- IO(println(s"Created ${id}... Ready again.")) *> IO(StdIn.readLine)
      list <- jobs.all()
      _ <- IO.println(s"here are your jobs: $list.  Now waiting") *> IO(StdIn.readLine)
      _ <- jobs.update(id, jobInfo.copy(title = "joe mamma"))
      newJob <- jobs.find(id)
      _ <- IO.println(s"now, here is the updated job after edit: $newJob.  Now waiting") *> IO(StdIn.readLine)
      _ <- jobs.delete(id)
      finalList <- jobs.all()
      _ <- IO(println(s"and here is the list after deletion: $finalList"))
    } yield ()
  }
}
