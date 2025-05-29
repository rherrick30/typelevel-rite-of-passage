package com.rockthejvm.jobsboard.core

import cats.*
import cats.effect.kernel.MonadCancelThrow
import cats.implicits.*
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.domain.pagination.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*

import java.util.UUID
import org.typelevel.log4cats.Logger

import com.rockthejvm.jobsboard.logging.syntax.*


trait Jobs[F[_]] {
  // "algebra"
  // CRUD operations
  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]

  def all(): F[List[Job]]

  def all(filter: JobFilter, pagination: Pagination) : F[List[Job]]

  def find(id: UUID): F[Option[Job]]

  def update(id: UUID, jobInfo: JobInfo): F[Option[Job]]

  def delete(id: UUID): F[Int]
}

/*
id: UUID,
date: Long,
ownerEmail: String,
company: String,
title: String,
description: String,
externalUrl: String,
remote: Boolean,
location: String,
salaryLo: Option[Int],
salaryHi: Option[Int],
currency: Option[String],
country: Option[String],
tags: Option[List[String]],
image: Option[String],
seniority: Option[String],
other: Option[String]
active: Boolean = false
*/
class LiveJobs[F[_] : MonadCancelThrow: Logger] private (xa: Transactor[F]) extends Jobs[F] {
  override def create(ownerEmail: String, jobInfo: JobInfo): F[UUID] =
    sql"""
      INSERT INTO JOBS(
            date,
            ownerEmail,
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
            other,
            active
            ) VALUES(
              ${System.currentTimeMillis()},
              ${ownerEmail},
              ${jobInfo.company},
              ${jobInfo.title},
              ${jobInfo.description},
              ${jobInfo.externalUrl},
              ${jobInfo.remote},
              ${jobInfo.location},
              ${jobInfo.salaryLo},
              ${jobInfo.salaryHi},
              ${jobInfo.currency},
              ${jobInfo.country},
              ${jobInfo.tags},
              ${jobInfo.image},
              ${jobInfo.seniority},
              ${jobInfo.other},
              ${false}
            )""".update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(xa)

  override def all(): F[List[Job]] =
    sql"""
        SELECT
          id,
          date,
          ownerEmail,
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
          other,
          active
        FROM jobs
      """.query[Job]
      .to[List]
      .transact(xa)

  override def all(filter: JobFilter, pagination: Pagination): F[List[Job]] = {
    val selectFragment = fr"""
        SELECT
          id,
          date,
          ownerEmail,
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
          other,
          active"""

    val fromFragment = fr"FROM jobs"

    val whereFragment = Fragments.whereAndOpt(
      filter.companies.toNel.map(companies => Fragments.in(fr"company", companies)),
      filter.locations.toNel.map(locations => Fragments.in(fr"location", locations)),
      filter.contries.toNel.map(contries => Fragments.in(fr"country", contries)),
      filter.seniorities.toNel.map(seniorities => Fragments.in(fr"seniority", seniorities)),

      filter.tags.toNel.map(tags =>
        Fragments.or(tags.toList.map(tag => fr"$tag=any(tags)"): _*)
      ),
      filter.maxSalary.map(salary => fr"salaryHi > $salary"),
      filter.remote.some.map(remote => fr"remote = $remote")
    )

    val paginationFragment = fr"ORDER BY id LIMIT ${pagination.limit} OFFSET ${pagination.offset}"

    val statement = selectFragment |+| fromFragment |+| whereFragment |+| paginationFragment

    Logger[F].info(statement.toString) *>
    statement.query[Job]
      .to[List]
      .transact(xa)
      .logError(e => s"Failed query: ${e.getMessage}")
  }

  override def find(id: UUID): F[Option[Job]] = sql"""
        SELECT
          id,
          date,
          ownerEmail,
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
          other,
          active
        FROM jobs
        WHERE id = $id
      """.query[Job]
    .option
    .transact(xa)

  override def update(id: UUID, jobInfo: JobInfo): F[Option[Job]] =
    sql"""
      UPDATE jobs SET
company =  ${jobInfo.company},
title =  ${jobInfo.title},
description =  ${jobInfo.description},
externalUrl =  ${jobInfo.externalUrl},
remote =  ${jobInfo.remote},
location =  ${jobInfo.location},
salaryLo =  ${jobInfo.salaryLo},
salaryHi =  ${jobInfo.salaryHi},
currency =  ${jobInfo.currency},
country =  ${jobInfo.country},
tags =  ${jobInfo.tags},
image =  ${jobInfo.image},
seniority =  ${jobInfo.seniority},
other =  ${jobInfo.other}
      WHERE id = $id
      """.update
      .run
      .transact(xa)
      .flatMap(- => find(id))

  override def delete(id: UUID): F[Int] =
    sql"""
          DELETE from jobs where id=$id
      """.update.run
      .transact(xa)
}

object LiveJobs {
  given jobRead: Read[Job] = Read[(
    UUID,
      Long,
      String,
      String,
      String,
      String,
      String,
      Boolean,
      String,
      Option[Int],
      Option[Int],
      Option[String],
      Option[String],
      Option[List[String]],
      Option[String],
      Option[String],
      Option[String],
      Boolean
    )].map {
    case (
      id: UUID,
      date: Long,
      ownerEmail: String,
      company: String,
      title: String,
      description: String,
      externalUrl: String,
      remote: Boolean,
      location: String,
      salaryLo: Option[Int] @unchecked,
      salaryHi: Option[Int] @unchecked,
      currency: Option[String] @unchecked,
      country: Option[String] @unchecked,
      tags: Option[List[String]] @unchecked,
      image: Option[String] @unchecked,
      seniority: Option[String] @unchecked,
      other: Option[String] @unchecked,
      active: Boolean
      ) => Job(id, date, ownerEmail, JobInfo(company, title, description, externalUrl, remote, location, salaryLo, salaryHi, currency, country, tags, image, seniority, other), active)
  }


  def apply[F[_] : MonadCancelThrow: Logger](xa: Transactor[F]): F[LiveJobs[F]] = new LiveJobs[F](xa).pure[F]
}