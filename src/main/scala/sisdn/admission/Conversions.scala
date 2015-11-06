package sisdn.admission

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import sisdn.common.User
import spray.json.DefaultJsonProtocol
import scala.language.implicitConversions
import scala.concurrent.duration.FiniteDuration

object Conversions {
  implicit def asFiniteDuration(d: java.time.Duration): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)
}

trait JsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val studentFormat = jsonFormat5(Student.apply)
  implicit val userFormat    = jsonFormat4(User.apply)
}

