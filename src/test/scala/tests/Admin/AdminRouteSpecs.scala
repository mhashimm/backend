package tests.Admin

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.model.headers.{OAuth2BearerToken, Authorization}
import akka.http.scaladsl.model.{StatusCodes, FormData}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import akka.util.Timeout
import akka.actor.Status.{Success => ActorSuccess, Failure => ActorFailure}
import authentikat.jwt.{JwtHeader, JsonWebToken, JwtClaimsSet}
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, FlatSpec}
import sisdn.Admin.Organization.{AddProgram, AddCourse, AddDepartment, AddFaculty}
import scala.concurrent.duration._
import sisdn.Admin.AdminRoutes
import sisdn.common.{SisdnUnauthorized, SisdnCreated, SisdnInvalid, User}
import scala.language.postfixOps

class AdminRouteSpecs extends FlatSpec with Matchers with ScalatestRouteTest {
  import AdminRouteSpecs._

  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val timeout: Timeout = 3 second

  def routeClass(actor: ActorRef) = new AdminRoutes(actor) {
    override val userExtractor = (str:String) => User("subject", "org", Some(Set(1)), Some(Set(1)))
  }

  "post path" should "respond to faculty creation with success status" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case AddFaculty(_, _) => sender() ! SisdnCreated }}))).route

    Post("/faculties", validFacForm).addHeader(hdr) ~> adminRoute ~> check{
      handled shouldBe true
      status shouldEqual StatusCodes.Created
    }
  }

  it should "fail to create department with proper response" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case AddDepartment(_,_) =>
        sender() ! SisdnInvalid("validation", "errors") }}))).route

    Post("/departments", validFacForm).addHeader(hdr) ~> adminRoute ~> check{
      handled shouldBe true
      status shouldEqual StatusCodes.BadRequest
    }
  }

  it should "respond with forbidden for unauthorized action in courses" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case AddCourse(_,_) =>
        sender() ! SisdnUnauthorized }}))).route
    val courseForm = FormData(Map("id" -> "x", "title" -> "title", "departmentId" -> "dep", "org" -> "org"))

    Post("/courses", courseForm).addHeader(hdr) ~> adminRoute ~> check{
      handled shouldBe true
      status shouldEqual StatusCodes.Unauthorized
    }
  }

  it should "properly unmarshal BigDecimal value in formFields" in {
    val adminRoute = routeClass(system.actorOf(Props(creator = new Actor() {
      override def receive = { case AddProgram(_, program) =>
        sender() ! SisdnCreated
        }}))).route

    val programForm = FormData(Map("id" -> "x", "title" -> "title", "facultyId" -> "facId",
      "terms" -> "8", "creditHours" -> "8.7", "org" -> "org"))

    Post("/programs", programForm).addHeader(hdr) ~> adminRoute ~> check{
      handled shouldBe true
    }
  }
}

object AdminRouteSpecs {
  val config = ConfigFactory.load()
  val key = config.getString("sisdn.key")

  val claimsSet = JwtClaimsSet("""{"departments" : [1], "subject" : "subject",
                                 |"org" : "org", "faculties" : [1]}""".stripMargin)
  val jwt: String = JsonWebToken(JwtHeader("HS256"), claimsSet, key)
  val hdr = Authorization(OAuth2BearerToken(jwt))

  val validFacForm = FormData( Map("id" -> "1", "title" -> "fac1", "org" -> "org1"))
}
