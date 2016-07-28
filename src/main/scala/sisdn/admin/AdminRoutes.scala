package sisdn.admin

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import sisdn.admin.Organization._
import sisdn.common._

class AdminRoutes(val router: ActorRef) extends Directives with UserJsonProtocol with OrgJsonProtocol{
  import AdminRoutes._
  implicit val system = ActorSystem("admin")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val timeout: Timeout = 3 second

  val queryRoute = new AdminQueryRoute().route

  val route =  { user: User =>
    pathPrefix("admin"){ queryRoute(user) ~
      path("faculties") {
        entity(as[Faculty]) { faculty =>
          authorize(authorizeAdmin(user, faculty)) {
            post {
              onSuccess(router ? AddFaculty(uuid, user, faculty.copy(org = Some(user.org)))) { adminPostPF }
            } ~
            put {
              onSuccess(router ? UpdateFaculty(uuid, user, faculty.copy(org = Some(user.org)))) { adminPostPF }
            }
          }
        }
      } ~
      path("departments") {
        entity(as[Department]){ department =>
          authorize(authorizeAdmin(user, department)) {
            post {
              onSuccess(router ? AddDepartment(uuid, user, department.copy(org = Some(user.org)))) { adminPostPF }
            } ~
            put {
              onSuccess(router ? UpdateDepartment(uuid, user, department.copy(org = Some(user.org)))) { adminPostPF }
            }
          }
        }
      } ~
      path("courses") {
        entity(as[Course]) { course =>
          authorize(authorizeAdmin(user, course)){
            post {
              onSuccess(router ? AddCourse(uuid, user, course.copy(org = Some(user.org)))) { adminPostPF }
            } ~
            put {
              onSuccess(router ? UpdateCourse(uuid, user, course.copy(org = Some(user.org)))) { adminPostPF }
            }
          }
        }
      } ~
      path("programs") {
        entity(as[Program]) { program =>
          authorize(authorizeAdmin(user, program)){
            post {
              onSuccess(router ? AddProgram(uuid, user, program.copy(org = Some(user.org)))) { adminPostPF }
            } ~
            put {
              onSuccess(router ? UpdateProgram(uuid, user, program.copy(org = Some(user.org)))) { adminPostPF }
            }
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

  def authorizeAdmin(user: User, entity: OrgEntity): Boolean = entity match {
    case e:Faculty => user.claims.exists(_.contains("admin_" + e.org.get))

    case e: Department => user.claims.exists(c => c.contains("admin_" + e.org.get) ||
      (c.contains("admin_" + e.facultyId) && e.org.get == user.org ))

    case e:Program => user.claims.exists(c => c.contains("admin_" + e.org.get) ||
      (c.contains("admin_" + e.facultyId) && e.org.get == user.org ))

    case e:Course => user.claims.exists(c => c.contains("admin_" + e.org.get) ||
      (c.contains("admin_" + e.facultyId) || c.contains("admin_" + e.departmentId)
        && e.org.get == user.org))
  }
}
