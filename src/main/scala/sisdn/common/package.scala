package sisdn

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.Unmarshaller
import spray.json.DefaultJsonProtocol
import scala.language.implicitConversions
import scala.concurrent.duration.FiniteDuration

package object common {
  def uuid = java.util.UUID.randomUUID.toString

  trait ObjectWithId { val id: String}

  implicit def asFiniteDuration(d: java.time.Duration): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)


  trait UserJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val userFormat = jsonFormat5(User.apply)
  }

  implicit val sisdnBigDecimalUnmarshaller: Unmarshaller[String, BigDecimal] =
    Unmarshaller.strict[String, BigDecimal] { string =>
      try BigDecimal(string)
      catch {
        case e:NumberFormatException => throw if (string.isEmpty) Unmarshaller.NoContentException
        else new IllegalArgumentException(s"'$string' is not a valid BigDecimal value", e)
      }
    }

  sealed trait SisdnReply extends ObjectWithId
  case class SisdnPending(id: String) extends SisdnReply
  case class SisdnAccepted(id: String) extends SisdnReply
  case class SisdnCreated(id: String) extends SisdnReply
  case class SisdnUnauthorized(id: String) extends SisdnReply
  case class SisdnNotFound(id: String) extends SisdnReply
  case class SisdnUpdated (id: String) extends SisdnReply
  case class SisdnInvalid(id: String, validationErrors: List[String]) extends SisdnReply
  object SisdnInvalid{
    def apply(id: String, message: String): SisdnInvalid    = SisdnInvalid(id, List(message))
    def apply(id: String, messages: String*) :SisdnInvalid  = SisdnInvalid(id, messages.toList)
  }

}