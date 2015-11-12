package sisdn.admission

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import sisdn.common.{UserJsonProtocol, User}
import spray.json.JsonParser
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class AdmissionRoute extends Directives with StudentJsonProtocol with UserJsonProtocol {
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
