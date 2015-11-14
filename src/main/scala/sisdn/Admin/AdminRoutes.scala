package sisdn.Admin

import akka.actor.{Props, ActorSystem, ActorRef}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.http.scaladsl.server.{Directives, Route}
import Directives._
import akka.stream.ActorMaterializer
import sisdn.Admin.Organization._
import sisdn.common.{UserJsonProtocol, User, SisdnCreated, SisdnInvalid, SisdnUnauthorized}
import sisdn.common.sisdnBigDecimalUnmarshaller //used implicitly by program route
import spray.json.JsonParser
import scala.concurrent.duration._
import akka.util.Timeout
import scala.language.postfixOps

class AdminRoutes(val router: ActorRef) extends Directives with UserJsonProtocol {

  import AdminRoutes._

  implicit val system = ActorSystem("admin")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val timeout: Timeout = 3 second
  val log = system.log


  val userExtractor = (token: String) => JsonParser(token).convertTo[User]

  val route = extractCredentials { bt => provide(userExtractor(bt.get.token)) { user => log.info("user {} has logged in", user.subject)
  path("faculties") {
    post {
      formFields(('id, 'title, 'titleTr.?, 'org, 'active.as[Boolean].?)).as(Faculty) { faculty =>
        onSuccess(router ? AddFaculty(user, faculty)) { adminPostPF }
      }
    }
  } ~
  path("departments") {
    post {
      formFields(('id, 'title, 'titleTr.?, 'org, 'active.as[Boolean].?)).as(Department) { department =>
        onSuccess(router ? AddDepartment(user, department)) { adminPostPF }
      }
    }
  } ~
  path("courses") {
    post {
      formFields(('id, 'title, 'titleTr.?, 'departmentId, 'remarks.?, 'org,
        'active.as[Boolean].?)).as(Course) { course =>
        onSuccess(router ? AddCourse(user, course)) { adminPostPF }
      }
    }
  } ~
  path("programs") {
    post {
      formFields(('id, 'title, 'titleTr.?, 'facultyId, 'terms.as[Int],
        'creditHours.as[BigDecimal], 'org, 'active.as[Boolean].?)).as(Program) { program =>
        onSuccess(router ? AddProgram(user, program)) { adminPostPF }
      }
    }
  }
  }
  }
}

object AdminRoutes {
  def adminPostPF = (reply:Any) => reply match {
    case SisdnCreated => complete(StatusCodes.Created)
    case SisdnInvalid(errors) => complete(StatusCodes.custom(400, errors.mkString(" ")))
    case SisdnUnauthorized => complete(StatusCodes.Unauthorized)
  }
}
