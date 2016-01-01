package tests.service

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtHeader}
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}
import sisdn.service.ServiceRoute
import sisdn.common.User

import scala.concurrent.Future

class ServiceRouteSpecs extends FlatSpec with Matchers with ScalatestRouteTest {
  import ServiceRouteSpecs._

  "ServiceRoutes" should "Return Success for Request with correct authorization" in {
    Post("/api").addHeader(hdr) ~> serviceRoutes ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  it should "Return \"Forbidden\" for absent Bearer Token code" in {
    Post("/api") ~> Route.seal(serviceRoutes) ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }

  it should "Reject all \"JWT\"s signed with the wrong secret" in {
    val jwt1 = JsonWebToken(JwtHeader("HS256"), claimsSet, "invalidSecret")
    val hdr1 = Authorization(OAuth2BearerToken(jwt1))
    Post("/api").addHeader(hdr1) ~> Route.seal(serviceRoutes) ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }

  it should "Reject all \"JWT\"s signed with malformed claimset" in {
    val invalidClaimsSet = JwtClaimsSet( """{"invalidUsername" : "subject", "org" : "org"}""")
    val invalidJWT = JsonWebToken(JwtHeader("HS256"), invalidClaimsSet, secret)
    val invalidHeader = Authorization(OAuth2BearerToken(invalidJWT))
    Post("/api").addHeader(invalidHeader) ~> Route.seal(serviceRoutes) ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }

  it should "Reject all none existing \"URL\"" in {
    Post("/noneexisitingurl").addHeader(hdr) ~> serviceRoutes ~> check {
      handled shouldBe false
    }
  }
}

object ServiceRouteSpecs {
  val secret = ConfigFactory.load().getString("sisdn.key")

  val claimsSet = JwtClaimsSet("""{"username" : "subject", "org" : "org"}""")
  val jwt: String = JsonWebToken(JwtHeader("HS256"), claimsSet, secret)

  val serviceRoutesClass = new ServiceRoute {
    override val innerRoutes = {user: User => onSuccess(Future.successful("")){ str =>  complete(StatusCodes.OK)}}
  }

  val serviceRoutes = serviceRoutesClass.serviceRoute

  val hdr = Authorization(OAuth2BearerToken(jwt))
}
