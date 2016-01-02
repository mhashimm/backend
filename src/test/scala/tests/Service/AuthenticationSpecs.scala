package tests.service

import authentikat.jwt._
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}
import sisdn.service.Authentication

class AuthenticationSpecs extends FlatSpec with Matchers {
  val theSecret = ConfigFactory.load().getString("sisdn.key")

  val auth = new Authentication {
    val secret = theSecret
  }


  val header = JwtHeader("HS256")
  val claimsSet = JwtClaimsSet("""{"username" : "subject", "org" : "org"}""")

  "Main service" should "Accept valid \"JWT\" and return a user" in {
    val jwt = JsonWebToken(header, claimsSet, theSecret)
    val result = auth.userExtractor(Some(jwt))
    result should not be None
    result.get.username shouldEqual "subject"
  }

  it should "return None for invalid \"JWT\"" in {
    // string missing username
    val invalidClaimsSet = JwtClaimsSet("""{"missingUserName" : "subject", "org" : "org"}""")
    val jwt = JsonWebToken(header, invalidClaimsSet, theSecret)

    val result = auth.userExtractor(Some(jwt))
    result shouldBe None
  }

  it should "return None for invalid \"KEY\"" in {
    val jwt = JsonWebToken(header, claimsSet, "invalidKey")

    val result = auth.userExtractor(Some(jwt))
    result shouldBe None
  }
}
