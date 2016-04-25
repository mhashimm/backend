package sisdn.admission

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

import sisdn.common._
import sisdn.admission.AdmissionUser.Admission

class AdmissionRoute(router: ActorRef) extends Directives with StudentJsonProtocol with UserJsonProtocol {
  import AdmissionRoute._
  implicit val system = ActorSystem("admission")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 3 second

  val route = { user: User =>
    path("admit" / "v1" | "admit") {
      post {
        entity(as[Student]) { student =>
          authorize(authorizeAdmission(user, student)) {
            onSuccess(router ? Admission(uuid, user, student)) { admissionPostPF }
          }
        }
      }
    }
  }
}

object AdmissionRoute {
  def admissionPostPF = (reply:Any) => reply match {
    case SisdnCreated(id) => complete(StatusCodes.Created)
    case SisdnUpdated(id) => complete(StatusCodes.OK)
    case SisdnInvalid(id, errors) => complete(StatusCodes.custom(400, errors.mkString(" ")))
    case SisdnUnauthorized(id) => complete(StatusCodes.Unauthorized)
    case SisdnDuplicate(id, errors) => complete(StatusCodes.custom(409, "Duplicate admission"))
  }

  def authorizeAdmission(user: User, student: Student) =
    user.claims.exists(claims => claims.contains("admitter_" + student.org)) ||
      (user.org == student.org && user.claims.exists(claims =>
        claims.contains("admitter_" + student.faculty)))
}
