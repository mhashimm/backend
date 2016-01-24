package sisdn.admin

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import akka.util.Timeout
import sisdn.common.User
import spray.json.DefaultJsonProtocol
import scala.concurrent.duration._
import scala.language.postfixOps
import slick.driver.PostgresDriver.api._
import spray.json._

class AdminQueryRoute extends Directives with AdminQuery with SprayJsonSupport with DefaultJsonProtocol {
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
            val response = for {
              crs  <- db.run(courses.filter(c => c.org === user.org && c.ts > coursets).result)
              deps <- db.run(departments.filter(d => d.org === user.org && d.ts > departmentts).result)
              facs <- db.run(faculties.filter(f => f.org === user.org && f.ts > facultyts).result)
              prgs <- db.run(programs.filter(p => p.org === user.org && p.ts > programts).result)
            } yield AdminQueryResponse(crs, deps, facs, prgs)

          response.map(_.toJson)
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
  Programs: Seq[ProgramRow]
)