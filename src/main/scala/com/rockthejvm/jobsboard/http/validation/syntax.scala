package com.rockthejvm.jobsboard.http.validation

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.*
import cats.data.Validated.{Invalid, Valid}
import cats.effect.Concurrent
import cats.implicits.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.typelevel.log4cats.*
import validators.*
import com.rockthejvm.jobsboard.logging.syntax.*
import com.rockthejvm.jobsboard.http.responses.*

object syntax {

    def validateEntity[A](entity: A)(using validator: Validator[A]) : ValidationResult[A] =
      validator.validate(entity)

    trait HttpValidationDsl[F[_]: MonadThrow : Logger] extends Http4sDsl[F] {
      extension (req: Request[F])
        def validate[A: Validator](serverLogicIfValid: A => F[Response[F]])(using EntityDecoder[F,A]) : F[Response[F]] =   {
          req.as[A]
            .logError(e => s"Parsing payload failed $e")
            .map(validateEntity)
            .flatMap {
              case Valid(value) => serverLogicIfValid(value)
              case Invalid(e) => BadRequest(FailureResponse(e.toList.map(_.errorMessage).mkString(", ")))
            }
        }
    }
}
