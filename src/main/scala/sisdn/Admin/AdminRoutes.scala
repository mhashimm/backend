package sisdn.Admin

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import sisdn.Admin.Organization._
import sisdn.common.{SisdnCreated, SisdnInvalid, SisdnUnauthorized, User, UserJsonProtocol, uuid}
import sisdn.common.sisdnBigDecimalUnmarshaller

class AdminRoutes(val router: ActorRef) extends Directives with UserJsonProtocol {
  import AdminRoutes._
  implicit val system = ActorSystem("admin")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val timeout: Timeout = 3 second

  val route =  { user: User =>
    path("faculties") {
      post {
        formFields(('id, 'title, 'titleTr.?, 'org ? user.org, 'active.as[Boolean].?)).as(Faculty) { faculty =>
          onSuccess(router ? AddFaculty(uuid, user, faculty)) { adminPostPF }
        }
      }
    } ~
    path("departments") {
      post {
        formFields(('id, 'title, 'titleTr.?, 'facultyId, 'org ? user.org, 'active.as[Boolean].?)).as(Department) { department =>
          onSuccess(router ? AddDepartment(uuid, user, department)) { adminPostPF }
        }
      }
    } ~
    path("courses") {
      post {
        formFields(('id, 'title, 'titleTr.?, 'departmentId, 'facultyId, 'remarks.?, 'org ? user.org,
          'active.as[Boolean].?)).as(Course) { course =>
          onSuccess(router ? AddCourse(uuid, user, course)) { adminPostPF }
        }
      }
    } ~
    path("programs") {
      post {
        formFields(('id, 'title, 'titleTr.?, 'facultyId, 'terms.as[Int],
          'creditHours.as[BigDecimal], 'org ? user.org, 'active.as[Boolean].?)).as(Program) { program =>
          onSuccess(router ? AddProgram(uuid, user, program)) { adminPostPF }
        }
      }
    }
  }
}

object AdminRoutes {
  def adminPostPF = (reply:Any) => reply match {
    case SisdnCreated(id) => complete(StatusCodes.Created)
    case SisdnInvalid(id, errors) => complete(StatusCodes.custom(400, errors.mkString(" ")))
    case SisdnUnauthorized(id) => complete(StatusCodes.Unauthorized)
  }
}
