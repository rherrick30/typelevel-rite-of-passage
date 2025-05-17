package com.rockthejvm.jobsboard.http.routes

import cats.*
import cats.effect.*
import cats.implicits.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.http.responses.*
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger

import java.util.UUID
import scala.collection.mutable

// Concurrent is needed instead of Monad or MonadThrow so that Circe can do its concurrent parsing
class JobRoutes[F[_] : Concurrent : Logger] private(jobs: Jobs[F]) extends Http4sDsl[F] {

  // POST /jobs?offset=x&limit=y {filters} // TODO add query params and filters later
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root => for {
      jobsList <- jobs.all()
      resp <- Ok(jobsList)
    } yield resp
  }

  // GET /jobs/uid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(jobId) => jobs.find(jobId) flatMap {
      case Some(job) => Ok(job)
      case None => NotFound(FailureResponse(s"job $jobId not found"))
    }
  }

  // POST /jobs {jobInfo}
  //  private def createJob(jobInfo: JobInfo) : F[Job] =
  //    Job (
  //      id = UUID.randomUUID(),
  //      date = System.currentTimeMillis(),
  //      ownerEmail = "TODO@rockthejvm.com",
  //      jobInfo = jobInfo,
  //      active = true
  //    ).pure[F]

  import com.rockthejvm.jobsboard.logging.syntax.*

  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req@POST -> Root / "create" => for {
      jobInfo <- req.as[JobInfo].logError(e => s"Parsing payload failed: $e")
      uuid <- jobs.create("TODO@rockthejvm.com", jobInfo)
      resp <- Created(uuid)
    } yield resp
  }

  // PUT /jobs/uuid {jobInfo}
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req@PUT -> Root / UUIDVar(jobId) => for {
      jobInfo <- req.as[JobInfo]
      maybeNewJob <- jobs.update(jobId, jobInfo)
      resp <- maybeNewJob match {
        case Some(job) => Ok()
        case None => NotFound(FailureResponse(s"job $jobId not found"))
      }
    } yield resp
  }

  // DELETE // jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req@DELETE -> Root / UUIDVar(jobId) => jobs.find(jobId) flatMap {
      case Some(_) => Ok(jobs.delete(jobId))
      case None => NotFound(FailureResponse(s"job $jobId not found"))
    }
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )
}

object JobRoutes {
  def apply[F[_] : Concurrent : Logger](jobs: Jobs[F]) = new JobRoutes[F](jobs)
}
