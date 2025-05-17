package com.rockthejvm.foundations

import cats.effect.*
import cats.implicits.*
import cats.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.*
import org.http4s.server.Router
import org.typelevel.ci.CIString

import java.util.UUID

object Http4s extends IOApp.Simple {

  // simulate an HTTP server with "students" and "courses"
  type Student = String

  case class Instructor(firstName: String, lastName: String)

  case class Course(id: String, title: String, year: Int, students: List[Student], instructorName: String)

  object CourseRepository {
    // a "database"
    private val catsEffectCourse: Course = Course(
      "5a103e43-1b6e-4060-8590-222a2e3672a5",
      "Rock the JVM Ultimate Scala course",
      2022, List("Daniel", "Robert"), "Martin Odersky")

    private val courses: Map[String, Course] = Map(catsEffectCourse.id -> catsEffectCourse)

    // API
    def findCoursesById(courseId: UUID): Option[Course] = courses.get(courseId.toString)

    def findCoursesByInstructor(name: String): List[Course] = courses.values.filter(_.instructorName == name).toList
  }

  // essential REST endpoints
  // GET localhost:8080/courses?instructor=Marting%20Odersky&year=2022
  // GET localhost:8080/courses/5a103e43-1b6e-4060-8590-222a2e3672a5/students

  object InstructorQueryParamMatcher extends QueryParamDecoderMatcher[String]("instructor")

  object YearQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")

  def courseRoutes[F[_] : Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "courses" :? InstructorQueryParamMatcher(instructor) +& YearQueryParamMatcher(perhapsYear) =>
        val courses = CourseRepository.findCoursesByInstructor(instructor)
        perhapsYear match {
          case Some(y) => y.fold(
            _ => BadRequest("parameter year is invalid"),
            year => Ok(courses.filter(_.year == year).asJson)
          )
          case None => Ok(courses.asJson)
        }

      case GET -> Root / "courses" / UUIDVar(courseId) / "students" =>
        CourseRepository.findCoursesById(courseId).map(_.students) match {
          case Some(students) => Ok(students.asJson, Header.Raw(CIString("my-custom-header"), "rock-th-jvm"))
          case None => NotFound(s"No course with $courseId was found")
        }
    }
  }

  def healthEndpoint[F[_] : Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*
    HttpRoutes.of[F] {
      case GET -> Root / "health" => Ok("All great here")
    }
  }

  def allRoutes[F[_] : Monad] = courseRoutes[F] <+> healthEndpoint[F]

  def routerWithPathPrefixes = Router(
    "/api" -> courseRoutes[IO],
    "/private" -> healthEndpoint[IO]
  ).orNotFound

  override def run = EmberServerBuilder
    .default[IO]
    .withHttpApp(routerWithPathPrefixes)
    .build
    .use(_ => IO.println("Server is ready") *> IO.never)
}
