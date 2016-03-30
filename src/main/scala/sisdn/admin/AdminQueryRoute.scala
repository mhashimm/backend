package sisdn.admin

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import akka.util.Timeout
import sisdn.admin._
import sisdn.common.User
import slick.driver.PostgresDriver.api._
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._
import scala.language.postfixOps

class AdminQueryRoute extends Directives with AdminQuery
    with SprayJsonSupport with DefaultJsonProtocol {
  implicit val system: ActorSystem = ActorSystem("AdminQueryRoute")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executor = system.dispatcher
  implicit val timeout: Timeout = 3 second

  implicit val courseFrmt     = jsonFormat9(CourseRow)
  implicit val departmentFrmt = jsonFormat7(DepartmentRow)
  implicit val facultyFrmt    = jsonFormat6(FacultyRow)
  implicit val programFrmt    = jsonFormat9(ProgramRow)
  implicit val responseFrmt   = jsonFormat4(AdminQueryResponse)

  val route = { user: User =>
    pathEndOrSingleSlash {
      get {
        parameters('coursets.as[Long] ? 0L, 'departmentts.as[Long] ? 0L, 'facultyts.as[Long] ? 0L,
          'programts.as[Long] ? 0L) {(coursets, departmentts, facultyts, programts) =>
          complete {
            val q = for {
              crs  <- courses.filter(c => c.org === user.org && c.ts > coursets).result
              deps <- departments.filter(d => d.org === user.org && d.ts > departmentts).result
              facs <- faculties.filter(f => f.org === user.org && f.ts > facultyts).result
              prgs <- programs.filter(p => p.org === user.org && p.ts > programts).result
            } yield (crs, deps, facs, prgs)

          db.run(q).map(r => AdminQueryResponse(r._1, r._2, r._3, r._4))
          }
        }
      }
    }
  }
}

case class AdminQueryResponse(
  courses: Seq[CourseRow],
  departments: Seq[DepartmentRow],
  faculties: Seq[FacultyRow],
  programs: Seq[ProgramRow]
)