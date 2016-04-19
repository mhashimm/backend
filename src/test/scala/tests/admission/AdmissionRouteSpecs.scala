package tests.admission

import sisdn.admission.AdmissionRoute
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtHeader}
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}
import MediaTypes._
import headers._
import sisdn.common.User

class AdmissionRouteSpecs extends FlatSpec with Matchers with ScalatestRouteTest {
  import AdmissionRouteSpecs._

  "Admission Service" should "Return Success for POST Request" in {
    Post("/admit", HttpEntity(`application/json`, stdJson)).addHeader(hdr) ~> admissionRoute ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  it should "Return MethodNotAllowed for GET Request" in {
    Get("/admit") ~> admissionRoute ~> check {
      rejection shouldEqual MethodRejection(HttpMethods.POST)
    }
  }

  it should """Accept request for /v1 route as the default route""" in {
    Post("/admit/v1", HttpEntity(`application/json`, stdJson)).addHeader(hdr) ~> admissionRoute ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  it should """Fail for arbitrary Url""" in {
    Post("/x", HttpEntity(`application/json`, "[]")) ~> admissionRoute ~> check {
      handled shouldBe false
    }
    Post("/admit/aa", HttpEntity(`application/json`, "[]")) ~> admissionRoute ~> check {
      handled shouldBe false
    }
    Post("/aa/admit", HttpEntity(`application/json`, "[]")) ~> admissionRoute ~> check {
      handled shouldBe false
    }
  }
}

object AdmissionRouteSpecs{
  val config = ConfigFactory.load()
  val key = config.getString("sisdn.key")
  val stdJson =
    """[{"id" : "1", "name" : "name", "thirdName" : "third",
      | "org" : "org", "faculty" : 1, "program" : 1}]""".stripMargin
  val claimsSet = JwtClaimsSet("""{"departments" : [1], "subject" : "subject",
                                 |"org" : "org", "faculties" : [1]}""".stripMargin)
  val jwt: String = JsonWebToken(JwtHeader("HS256"), claimsSet, key)

  val routeClass = new AdmissionRoute

  val admissionRoute = routeClass.route(User("subject", "org", None, None, None))
  val hdr = Authorization(OAuth2BearerToken(jwt))
}

