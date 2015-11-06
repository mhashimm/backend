package sisdn.admission.service

import akka.actor.{Props, ActorSystem}
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import sisdn.admission.model.{User, Student}
import sisdn.admission.service.AdmissionUser.Admit
import sisdn.admission.utils.JsonProtocol
import spray.json.JsonParser
import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import scala.concurrent.duration._

class AdmissionRoute extends Directives with JsonProtocol {
  implicit val system = ActorSystem("admission")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 1 second

  val userExtractor = (token: String) => JsonParser(token).convertTo[User]

  val route: Route = path("admit" / "v1" | "admit") {
    post {
      extractCredentials { bt =>
        provide(userExtractor(bt.get.token)) { user =>
          entity(as[List[Student]]) { students =>
            complete {
              ""
            }
          }
        }
      }
    }
  }
}
