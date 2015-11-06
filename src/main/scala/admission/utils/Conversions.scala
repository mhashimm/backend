package sisdn.admission.utils

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import sisdn.admission.model.{User, Student}
import spray.json._


object Conversions {
  implicit def asFiniteDuration(d: java.time.Duration): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)
}

trait JsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val studentFormat = jsonFormat5(Student.apply)
  implicit val userFormat    = jsonFormat4(User.apply)
}
