package sisdn

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.Unmarshaller
import spray.json.DefaultJsonProtocol
import scala.language.implicitConversions
import scala.concurrent.duration.FiniteDuration

package object common {
  implicit def asFiniteDuration(d: java.time.Duration): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)

  trait UserJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val userFormat = jsonFormat4(User.apply)
  }

  implicit val sisdnBigDecimalUnmarshaller: Unmarshaller[String, BigDecimal] =
    Unmarshaller.strict[String, BigDecimal] { string =>
      try BigDecimal(string)
      catch {
        case e:NumberFormatException => throw if (string.isEmpty) Unmarshaller.NoContentException
        else new IllegalArgumentException(s"'$string' is not a valid BigDecimal value", e)
      }
    }

  sealed trait SisdnReply
  case object SisdnAccepted extends SisdnReply
  case object SisdnCreated extends SisdnReply
  case object SisdnUnauthorized extends SisdnReply
  case class SisdnInvalid(validationErrors: List[String]) extends SisdnReply
  object SisdnInvalid{
    def apply(message: String): SisdnInvalid    = new SisdnInvalid(List(message))
    def apply(messages: String*) :SisdnInvalid  = new SisdnInvalid(messages.toList)
  }

}