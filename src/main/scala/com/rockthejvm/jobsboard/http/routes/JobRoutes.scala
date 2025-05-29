package com.rockthejvm.jobsboard.http.routes

import cats.*
import cats.effect.*
import cats.implicits.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.domain.pagination.*
import com.rockthejvm.jobsboard.http.responses.*
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import com.rockthejvm.jobsboard.logging.syntax.*
import java.util.UUID
import scala.collection.mutable
import com.rockthejvm.jobsboard.http.validation.syntax.*

// Concurrent is needed instead of Monad or MonadThrow so that Circe can do its concurrent parsing
class JobRoutes[F[_] : Concurrent : Logger] private(jobs: Jobs[F]) extends HttpValidationDsl[F] {

  object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
  object LimitQueryParam extends OptionalQueryParamDecoderMatcher[Int]("limit")

  // POST /jobs?limit=x&loffset=y {filters} // TODO add query params and filters later
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root :? LimitQueryParam(limit) +& OffsetQueryParam(offset) => for {
      filter <- req.as[JobFilter]
      jobsList <- jobs.all(filter, Pagination(limit, offset))
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



  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req@POST -> Root / "create" =>
      req.validate[JobInfo] { jobInfo =>
        for {
          uuid <- jobs.create("TODO@rockthejvm.com", jobInfo)
          resp <- Created(uuid)
        } yield resp
      }
  }

  // PUT /jobs/uuid {jobInfo}
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req@PUT -> Root / UUIDVar(jobId) =>
      req.validate[JobInfo] { jobInfo =>
        for {
          maybeNewJob <- jobs.update(jobId, jobInfo)
          resp <- maybeNewJob match {
            case Some(job) => Ok()
            case None => NotFound(FailureResponse(s"job $jobId not found"))
          }
        } yield resp

      }
  }

  // DELETE // jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req@DELETE -> Root / UUIDVar(jobId) => jobs.find(jobId).flatMap {
      case Some(job) =>
        for {
          _    <- jobs.delete(jobId)
          resp <- Ok()
        } yield resp
      case None => NotFound(FailureResponse(s"Cannot delete job $jobId: not found"))
    }
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )
}

object JobRoutes {
  def apply[F[_] : Concurrent : Logger](jobs: Jobs[F]) = new JobRoutes[F](jobs)
}
