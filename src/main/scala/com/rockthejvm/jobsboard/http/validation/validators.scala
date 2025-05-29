package com.rockthejvm.jobsboard.http.validation

import cats.*
import cats.data.*
import cats.data.Validated.*
import com.rockthejvm.jobsboard.domain.job.*
import cats.implicits.*
import java.net.URL
import scala.util.{Try, Success, Failure}
import scala.util.matching.Regex

object validators {

  sealed trait ValidationFailure(val errorMessage: String)
  case class EmptyField(fieldName: String) extends ValidationFailure(s"'$fieldName' is empty")
  case class InvalidUrl(fieldName: String) extends ValidationFailure(s"'$fieldName' contains an invalid Url")
  // empty field, invalid URL, invalid email, etc...

  type ValidationResult[A] = ValidatedNel[ValidationFailure, A]

  trait Validator[A] {
    def validate(value: A) : ValidationResult[A]
  }

  def validateRequired[A](field: A, fieldName: String)(required: A => Boolean) : ValidationResult[A] =
    if(required(field)) field.validNel
    else EmptyField(fieldName).invalidNel

  val urlPattern : Regex = "https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)".r
  def validateUrl[A](field: A, fieldName: String)(validUrl: A => Boolean) : ValidationResult[A] =
    if(validUrl(field)) field.validNel
    else InvalidUrl(fieldName).invalidNel

  def validateUrlDaniel(field: String, fieldName: String): ValidationResult[String] =
    Try(URL(field).toURI()) match {
      case Success(_) => field.validNel
      case Failure(e) => InvalidUrl(fieldName).invalidNel
    }

  given jobInfoValidator: Validator[JobInfo] = (jobInfo: JobInfo) => {
    val JobInfo(
      company,
      title,
      description,
      externalUrl,
      remote,
      location,
      salaryLo,
      salaryHi,
      currency,
      country,
      tags,
      image,
      seniority,
      other
    ) = jobInfo

    val validCompany = validateRequired(company, "company")(_.nonEmpty)
    val validTitle = validateRequired(title, "title")(_.nonEmpty)
    val validDescription = validateRequired(description, "description")(_.nonEmpty)
    val validExternalUrlDaniel = validateUrlDaniel(externalUrl, "externalUrl")
    val validLocation = validateRequired(location, "location")(_.nonEmpty)
    //val validExternalUrlRob = validateUrl(externalUrl, "externalUrl")(urlPattern.matches(_))

    (
      validCompany,
      validTitle,
      validDescription,
      validExternalUrlDaniel,
      remote.validNel,
      validLocation,
      salaryLo.validNel,
      salaryHi.validNel,
      currency.validNel,
      country.validNel,
      tags.validNel,
      image.validNel,
      seniority.validNel,
      other.validNel
    ).mapN(JobInfo.apply) // ValidatedNel[ValidationFailure, JobInfo]

  }
}
