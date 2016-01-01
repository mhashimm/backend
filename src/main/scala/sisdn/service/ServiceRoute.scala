package sisdn.service

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.HttpCredentials
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import authentikat.jwt.JsonWebToken
import com.typesafe.config.ConfigFactory
import sisdn.Admin.{AdminRouter, AdminRoutes}
import sisdn.common.{User, UserJsonProtocol}
import spray.json.JsonParser

class ServiceRoute extends UserJsonProtocol {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val config = ConfigFactory.load()
  val secret = config.getString("sisdn.key")

  val router = system.actorOf(Props(classOf[AdminRouter]))
  val admin = new AdminRoutes(router)

  def userExtractor(cred: Option[String]) = cred match {
    case Some(JsonWebToken(_, claimsSet, _)) if JsonWebToken.validate(cred.get, secret) => {
      try {
        Some(JsonParser(claimsSet.asJsonString).convertTo[User])
      }
      catch {
        case _: Throwable => None
      }
    }
    case _ => None
  }

  val innerRoutes =  admin.route

  val serviceRoute = pathPrefix("api") {
    extractCredentials{ bt: Option[HttpCredentials] =>
      provide(userExtractor(bt.map(_.token()))) { user =>
        authorize(user.isDefined) {
          innerRoutes(user.get)
        }
      }
    }
  }

  def main(args: String*) = Http().bindAndHandle(serviceRoute, "localhost", 9000)
}


