package sisdn

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import scala.language.implicitConversions
import scala.concurrent.duration.FiniteDuration

package object common {
  implicit def asFiniteDuration(d: java.time.Duration): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)


  trait UserJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val userFormat = jsonFormat4(User.apply)
  }

  sealed trait SisdnReply
  case object SisdnAccepted extends SisdnReply
  case object SisdnCreated extends SisdnReply
  case object SisdnUnauthorized extends SisdnReply
  case class SisdnInvalid(validationErrors: List[String] = Nil) extends SisdnReply

}