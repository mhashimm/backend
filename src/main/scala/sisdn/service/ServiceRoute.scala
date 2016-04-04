package sisdn.service

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.headers.HttpCredentials
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.stream.javadsl.Sink
import com.typesafe.config.ConfigFactory
import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import sisdn.admin._
import slick.driver.MySQLDriver.api._
import sisdn.admin.AdminQueryRoute

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
        complete((Forbidden, "غير مسموح باجراء العملية المطلوبة"))
      }.result

  private def addAccessControlHeaders = mapResponseHeaders { headers =>
    `Access-Control-Allow-Origin`(allowedOrigin) +:
      `Access-Control-Allow-Headers`("Authorization", "Content-Type",
        "pragma", "cache-control", "X-Requested-With") +: headers
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

object ServiceEndpoint extends ServiceRoute with AdminQuery {
  val config = ConfigFactory.load()
  val secret = config.getString("sisdn.key")
  override val allowedOrigins = config.getString("sisdn.cors.allowedOrigins")
  val appEnv = config.getString("sisdn.appEnv")

  def main(args: Array[String]) {

    val queries = PersistenceQuery(system).readJournalFor[LeveldbReadJournal](
      LeveldbReadJournal.Identifier)

    db.run(streamOffsets.result).map{ result => result.map{ os =>
      queries.eventsByPersistenceId (os._1, os._2, Long.MaxValue)
      .mapAsync (1) { writeToDB }
      .runWith (Sink.ignore)
      }
    }

    Http().bindAndHandle(serviceRoute, "localhost", 8080)
  }
}


