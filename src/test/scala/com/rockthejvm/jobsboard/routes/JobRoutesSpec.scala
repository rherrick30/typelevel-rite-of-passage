package com.rockthejvm.jobsboard.routes

import cats.effect.*
import cats.implicits.*
import org.http4s.*
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.job
import com.rockthejvm.jobsboard.domain.job.Job
import com.rockthejvm.jobsboard.fixtures.*
import com.rockthejvm.jobsboard.http.routes.JobRoutes
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID

class JobRoutesSpec
  extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with Http4sDsl[IO]
  with JobFixture {

  given Logger[IO] = Slf4jLogger.getLogger[IO]

  ////////////////////////////////////////////////////////////
  // prep
  ////////////////////////////////////////////////////////////
  val jobs: Jobs[IO] = new Jobs[IO] {
    override def create(ownerEmail: String, jobInfo: job.JobInfo): IO[UUID] = IO{ NewJobUuid }

    override def all(): IO[List[job.Job]] = IO{ List(AwesomeJob) }

    override def find(id: UUID): IO[Option[job.Job]] = if(id == NewJobUuid) IO{ Option(AwesomeJob) } else IO.pure(None)

    override def update(id: UUID, jobInfo: job.JobInfo): IO[Option[job.Job]] = if(id == AwesomeJobUuid) IO{ Option(UpdatedAwesomeJob) } else IO.pure(None)

    override def delete(id: UUID): IO[Int] = IO { if (id == AwesomeJobUuid) 1 else 0 }
  }

  val jobsRoutes: HttpRoutes[IO] = JobRoutes[IO](jobs).routes

  ////////////////////////////////////////////////////////////
  // TESTS
  ////////////////////////////////////////////////////////////
        // simulate HTTP request
        // get HHT response
        // make some assertiong

  "Job Routes"  - {
    "should return a job with a given id" in {
      // code under test
      for {
        response <- jobsRoutes.orNotFound.run (
          Request(method = Method.GET, uri = uri"/jobs/efcd2a64-4463-453a-ada8-b1bae1db4377")
        )
        retreived <- response.as[Job]
      } yield {
        response.status shouldBe Status.Ok
        retreived shouldBe AwesomeJob
       }
      }

    "should return all jobs" in {
      // code under test
      for {
        response <- jobsRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs")
        )
        retreived <- response.as[List[Job]]
      } yield {
        response.status shouldBe Status.Ok
        retreived shouldBe List(AwesomeJob)
      }
    }

    "should create a new job" in {
      // code under test
      for {
        response <- jobsRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs/create")
            .withEntity(AwesomeJob.jobInfo)
        )
        retreived <- response.as[UUID]
      } yield {
        response.status shouldBe Status.Created
        retreived shouldBe NewJobUuid
      }
    }

    "should only update a job that exists" in {
      // code under test
      for {
        response <- jobsRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
        badResponse <- jobsRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40065")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
      } yield {
        response.status shouldBe Status.Ok
        badResponse.status shouldBe Status.NotFound
      }
    }

    "should only delete a job that exists" in {
      // code under test
      for {
        response <- jobsRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
        )
        badResponse <- jobsRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40065")
        )
      } yield {
        response.status shouldBe Status.Ok
        badResponse.status shouldBe Status.NotFound
      }
    }

  }Pa
}
