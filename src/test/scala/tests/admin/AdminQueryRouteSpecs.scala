package tests.admin

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.scalatest.{Matchers, FlatSpec}
import scala.concurrent.duration._
import scala.language.postfixOps
import sisdn.admin.OrgJsonProtocol
import sisdn.common.User

class AdminQueryRouteSpecs extends FlatSpec with Matchers with ScalatestRouteTest with OrgJsonProtocol {

  import sisdn.admin.AdminQueryRoute

  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val timeout: Timeout = 3 second

  val user = User("subject", "some-org", None, None, None)

  val queryRoute =  new AdminQueryRoute().route

  ignore should "respond with all entities for get request without parameters" in {

    Get("/") ~> queryRoute(user) ~> check{
      handled shouldBe true
      
      println(responseAs[String])
    }
  }

}
