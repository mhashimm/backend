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
import sisdn.common._

class AdminRoutes(val router: ActorRef) extends Directives with UserJsonProtocol with OrgJsonProtocol{
  import AdminRoutes._
  implicit val system = ActorSystem("admin")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val timeout: Timeout = 3 second

  val route =  { user: User =>
    pathPrefix("admin"){
      path("faculties") {
        post {
          entity(as[Faculty]) { faculty =>
            onSuccess(router ? AddFaculty(uuid, user, faculty.copy(org = Some(user.org)))) { adminPostPF }
          }
        } ~
        put {
          entity(as[Faculty]) { faculty =>
            onSuccess(router ? UpdateFaculty(uuid, user, faculty.copy(org = Some(user.org)))) { adminPostPF }
          }
        }
      } ~
      path("departments") {
        post {
          entity(as[Department]) { department =>
            onSuccess(router ? AddDepartment(uuid, user, department.copy(org = Some(user.org)))) { adminPostPF }
          }
        } ~
        put {
          entity(as[Department]) { department =>
            onSuccess(router ? UpdateDepartment(uuid, user, department.copy(org = Some(user.org)))) { adminPostPF }
          }
        }
      } ~
      path("courses") {
        post {
          entity(as[Course]) { course =>
            onSuccess(router ? AddCourse(uuid, user, course.copy(org = Some(user.org)))) { adminPostPF }
          }
        } ~
        put {
          entity(as[Course]) { course =>
            onSuccess(router ? UpdateCourse(uuid, user, course.copy(org = Some(user.org)))) { adminPostPF }
          }
        }
      } ~
      path("programs") {
        post {
          entity(as[Program]) { program =>
            onSuccess(router ? AddProgram(uuid, user, program.copy(org = Some(user.org)))) { adminPostPF }
          }
        } ~
        put {
          entity(as[Program]) { program =>
            onSuccess(router ? UpdateProgram(uuid, user, program.copy(org = Some(user.org)))) { adminPostPF }
          }
        }
      }
    }
  }
}

object AdminRoutes {
  def adminPostPF = (reply:Any) => reply match {
    case SisdnCreated(id) => complete(StatusCodes.Created)
    case SisdnUpdated(id) => complete(StatusCodes.OK)
    case SisdnInvalid(id, errors) => complete(StatusCodes.custom(400, errors.mkString(" ")))
    case SisdnUnauthorized(id) => complete(StatusCodes.Unauthorized)
    case SisdnNotFound(id) => complete(StatusCodes.NotFound)
  }
}
