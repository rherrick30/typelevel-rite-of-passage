package com.rockthejvm.jobsboard.core

import cats.data.AndThen
import cats.effect.*
import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import com.rockthejvm.jobsboard.domain.job.JobFilter
import com.rockthejvm.jobsboard.domain.pagination.Pagination
import org.scalatest.matchers.should.Matchers
import com.rockthejvm.jobsboard.fixtures.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import org.http4s.Uri.Path
import org.http4s.internal.CharPredicate
import org.scalatest.matchers.{BeMatcher, BePropertyMatcher, HavePropertyMatcher, Matcher}
import pureconfig.ConfigFieldMapping

import scala.collection.SetOps
import scala.concurrent.impl.{FutureConvertersImpl, Promise}
import scala.jdk.FunctionWrappers
import scala.runtime.{AbstractFunction1, AbstractPartialFunction}
import scala.runtime.function.JProcedure1
import scala.xml.transform.BasicTransformer
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class JobsSpec
  extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with DoobieSpec
  with JobFixture {


  given Logger[IO] = Slf4jLogger.getLogger[IO]

  val initScript = "sql/jobs.sql"

  "jobs 'algebra'" - {

    "should return no job if the specified UUID does not exist" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          retrieved <- jobs.find(NotFoundJobUuid)
        } yield retrieved

        program.asserting(_ shouldBe None)

      }
    }

    "should return a job by id" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          retrieved <- jobs.find(AwesomeJobUuid)
        } yield retrieved

        program.asserting(_ shouldBe Some(AwesomeJob))

      }
    }

    "should return all jobs" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          retrieved <- jobs.all()
        } yield retrieved

        program.asserting(_ shouldBe List(AwesomeJob))

      }
    }

    "should create a new job" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          jobId <- jobs.create("joe@mamma.com", RockTheJvmNewJob)
          maybeJob <- jobs.find(jobId)
        } yield maybeJob

        program.asserting(_.map(_.jobInfo) shouldBe Some(RockTheJvmNewJob))
      }
    }

    "should return an updated job" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          maybeJob <- jobs.update(AwesomeJobUuid, UpdatedAwesomeJob.jobInfo)
        } yield maybeJob

        program.asserting(_ shouldBe Some(UpdatedAwesomeJob))
      }
    }

    "should not return an updated job when not found" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          maybeJob <- jobs.update(NotFoundJobUuid, UpdatedAwesomeJob.jobInfo)
        } yield maybeJob

        program.asserting(_ shouldBe None)
      }
    }

    "should return 1 when deleting" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          noDeleted <- jobs.delete(AwesomeJobUuid)
        } yield noDeleted

        program.asserting(_ shouldBe 1)
      }
    }

    "should return 0 when deleting a non-existent" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          noDeleted <- jobs.delete(NotFoundJobUuid)
        } yield noDeleted

        program.asserting(_ shouldBe 0)
      }
    }

    "should verify deletion" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          numberOfDeletedJobs <- jobs.delete(AwesomeJobUuid)
          countOfRemainingJobs <- sql"select count(1) from jobs where id=${AwesomeJobUuid}".query[Int].unique.transact(xa)
        } yield (numberOfDeletedJobs, countOfRemainingJobs)

        program.asserting {
          case  (numberOfDeletedJobs, countOfRemainingJobs) =>
            numberOfDeletedJobs shouldBe 1
            countOfRemainingJobs shouldBe 0
        }
      }

    }

    "should filter remote jobs" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          filteredJobs <- jobs.all(JobFilter(remote=true), Pagination.default)
        } yield filteredJobs

        program.asserting(_ shouldBe List())

      }
    }

    "should filter jobs by tags" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          filteredJobs <- jobs.all(JobFilter(tags = List("cats","dogs")), Pagination.default)
        } yield filteredJobs

        program.asserting(_ shouldBe List(AwesomeJob))

      }
    }


  }


}
