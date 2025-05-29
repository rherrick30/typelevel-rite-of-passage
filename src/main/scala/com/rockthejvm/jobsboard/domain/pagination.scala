package com.rockthejvm.jobsboard.domain

object pagination {
  final case class Pagination(limit: Int, offset: Int)
  object Pagination {
    // TODO: Get these values from the config file
    val defaultPageSize = 20
    val defaultOffset = 0
    def apply(maybeLimit: Option[Int], maybeOffset: Option[Int]) : Pagination =
      Pagination(limit = maybeLimit.getOrElse(defaultPageSize), offset = maybeOffset.getOrElse(defaultOffset))
    def default : Pagination = Pagination(None, None)
  }
}
