package sisdn.Admin

import akka.actor.{Props, ActorSystem, ActorRef}
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask

import akka.http.scaladsl.server.{Directives, Route}
import Directives._
import akka.stream.ActorMaterializer
import sisdn.Admin.Organization.{AddDepartment, Department, AddFaculty, Faculty}
import sisdn.common._
import spray.json.JsonParser
import scala.concurrent.duration._
import akka.util.Timeout
import scala.language.postfixOps
import scala.concurrent.ExecutionContext

class AdminRoutes(val router: ActorRef) extends Directives with UserJsonProtocol {
  import AdminRoutes._
  implicit val system = ActorSystem("admin")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 3 second

  val userExtractor = (token: String) => JsonParser(token).convertTo[User]

  val route = path("faculties") {
    post {
      extractCredentials { bt =>
        provide(userExtractor(bt.get.token)) { user =>
          formFields(('id, 'title, 'titleTr.?, 'org)).as(Faculty) { faculty =>
            onSuccess(router ? AddFaculty(user, faculty)) { adminPostPF }
          }
        }
      }
    }
  } ~
  path("departments"){
    post{
      extractCredentials { bt =>
        provide(userExtractor(bt.get.token)) { user =>
          formFields(('id, 'title, 'titleTr.?, 'org)).as(Department) { department =>
            onSuccess(router ? AddDepartment(user, department)) { adminPostPF }
          }
        }
      }
    }
  }/*~
  path("courses"){
    post{}
  }*/
}

object AdminRoutes {
  def adminPostPF = (reply:Any) => reply match {
    case SisdnCreated => complete(StatusCodes.Created)
    case SisdnInvalid(errors) => complete(StatusCodes.custom(400, errors.mkString(" ")))
    case SisdnUnauthorized => complete(StatusCodes.Unauthorized)
  }
}
