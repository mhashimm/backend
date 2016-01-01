package tests.service

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{OAuth2BearerToken, Authorization}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import authentikat.jwt._
import com.typesafe.config.{ConfigResolveOptions, ConfigParseOptions, ConfigFactory}
import sisdn.common.User
import scala.concurrent.duration._
import org.scalatest.{Matchers, FlatSpec}
import sisdn.service.ServiceRoute

class AuthenticationSpecs extends FlatSpec with Matchers {

  val service = new ServiceRoute
  val secret = ConfigFactory.load().getString("sisdn.key")

  val header = JwtHeader("HS256")
  val claimsSet = JwtClaimsSet("""{"username" : "subject", "org" : "org"}""")

  "Main service" should "Accept valid \"JWT\" and return a user" in {
    val jwt = JsonWebToken(header, claimsSet, secret)
    val result = service.userExtractor(Some(jwt))
    result should not be None
    result.get.username shouldEqual "subject"
  }

  it should "return None for invalid \"JWT\"" in {
    // string missing username
    val invalidClaimsSet = JwtClaimsSet("""{"missingUserName" : "subject", "org" : "org"}""")
    val jwt = JsonWebToken(header, invalidClaimsSet, secret)

    val result = service.userExtractor(Some(jwt))
    result shouldBe None
  }

  it should "return None for invalid \"KEY\"" in {
    val jwt = JsonWebToken(header, claimsSet, "invalidKey")

    val result = service.userExtractor(Some(jwt))
    result shouldBe None
  }
}
