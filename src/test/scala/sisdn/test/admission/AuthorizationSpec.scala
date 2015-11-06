package sisdn.admission.test

import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.RequestContext
import org.scalatest.{FlatSpec, Matchers}
import headers._
import sisdn.admission.utils._

import scala.concurrent.Promise

class AuthorizationSpec extends FlatSpec with Matchers {
//  object ctx extends RequestContext(HttpRequest(
//    HttpMethod.custom(""),
//    Uri(),
//    Seq(Authorization(OAuth2BearerToken("")))
//  ), Promise.successful(HttpResponse), 5){
//    val request = HttpRequest
//
//  }

}
