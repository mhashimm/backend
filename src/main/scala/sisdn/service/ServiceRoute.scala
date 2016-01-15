package sisdn.service

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.headers.HttpCredentials
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentType,  MediaTypes, HttpResponse}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import sisdn.Admin.{AdminRouter, AdminRoutes}

trait ServiceRoute extends Directives with Authentication {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  val allowedOrigins: String

  lazy val allowedOrigin = HttpOrigin(allowedOrigins)


  val router = system.actorOf(Props(classOf[AdminRouter]))
  val admin = new AdminRoutes(router)
  val innerRoutes = admin.route

  implicit def sisdnRejectionHandler =
    RejectionHandler.newBuilder()
      .handle { case AuthorizationFailedRejection =>
        complete((Forbidden, "You're out of your depth!"))
      }.result

  private def addAccessControlHeaders = mapResponseHeaders { headers =>
    `Access-Control-Allow-Origin`(allowedOrigin) +:
      `Access-Control-Allow-Headers`("Authorization", "Content-Type",
        "X-Requested-With") +: headers
  }

  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(200).withHeaders(
      `Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)
    )
    )
  }

  def corsHandler(r: Route) = addAccessControlHeaders {
    preflightRequestHandler ~ r
  }

  val serviceRoute = corsHandler {
    handleRejections(sisdnRejectionHandler) {
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
}

object ServiceEndpoint extends ServiceRoute {
  val config = ConfigFactory.load()
  val secret = config.getString("sisdn.key")
  override val allowedOrigins = config.getString("sisdn.cors.allowed-origins")
  val appEnv = config.getString("sisdn.appEnv")

  def main(args: Array[String]) {
    Http().bindAndHandle(serviceRoute, "localhost", 8888)
  }
}


