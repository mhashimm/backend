package sisdn.service

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.HttpCredentials
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import sisdn.Admin.{AdminRouter, AdminRoutes}

trait ServiceRoute extends Directives with Authentication {
  implicit val system = ActorSystem()
  implicit val executor =  system.dispatcher
  implicit val materializer = ActorMaterializer()

  val router = system.actorOf(Props(classOf[AdminRouter]))
  val admin = new AdminRoutes(router)

  val innerRoutes =  admin.route
  val resources = path("") {
    getFromResource("dist/index.html")
  } ~ getFromResourceDirectory("dist")

  implicit def sisdnRejectionHandler =
    RejectionHandler.newBuilder()
      .handle { case AuthorizationFailedRejection =>
        complete((Forbidden, "You're out of your depth!"))
      }.result


  val serviceRoute = handleRejections(sisdnRejectionHandler){
    extractCredentials { bt: Option[HttpCredentials] =>
      provide(userExtractor(bt.map(_.token()))) { user =>
        pathPrefix("api") {
          authorize(user.isDefined) {
            innerRoutes(user.get)
          }
        } ~ path("") {
          getFromResource("dist/index.html")
        } ~
          getFromResourceDirectory("dist")
      }
    }
  }
}

object ServiceEndpoint extends ServiceRoute {
  val config = ConfigFactory.load()
  val secret = config.getString("sisdn.key")
  val appEnv = config.getString("sisdn.appEnv")

  def main(args: Array[String]) {
    Http().bindAndHandle(serviceRoute, "localhost", 9000)
  }
}


