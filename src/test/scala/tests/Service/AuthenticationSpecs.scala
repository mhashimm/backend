package tests.service

import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}
import sisdn.service.Authentication
import tests.service.jwts._

class AuthenticationSpecs extends FlatSpec with Matchers {
  val config = ConfigFactory.load()
  val theSecret = config.getString("sisdn.key")

  val auth = new Authentication {
    val secret = theSecret
    val appEnv = config.getString("sisdn.appEnv")
  }

  "Main service" should "Accept valid \"JWT\" and return a user" in {
    val result = auth.userExtractor(Some(validJWT))
    result should not be None
    result.get.username shouldEqual "mhashim"
  }

  it should "return None for invalid \"JWT\"" in {
    val result = auth.userExtractor(Some(invalidJWT))
    result shouldBe None
  }

  it should "return None for invalid \"KEY\"" in {
    val result = auth.userExtractor(Some(wrongKeyJWT))
    result shouldBe None
  }

  //TODO try to add jwt dynamically istead of relying on static ones
  it should "fail for expired \"JWT\"s" in {
    val auth = new Authentication {
      override val secret: String = ConfigFactory.load().getString("sisdn.key")
      override val appEnv: String = "dist"
    }

    val result = auth.userExtractor(Some(validJWT))
    result shouldBe None
  }
}
