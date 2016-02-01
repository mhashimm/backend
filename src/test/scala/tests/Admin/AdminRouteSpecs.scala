package tests.admin

import akka.actor.Status.{Failure => ActorFailure, Success => ActorSuccess}
import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.scalatest.{FlatSpec, Matchers}
import sisdn.admin.Organization._
import sisdn.admin.{AdminRoutes, OrgJsonProtocol}
import sisdn.common._

import scala.concurrent.duration._
import scala.language.postfixOps

class AdminRouteSpecs extends FlatSpec with Matchers with ScalatestRouteTest with OrgJsonProtocol {
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val timeout: Timeout = 3 second

  val user = User("subject", "org", None, None, None)

  def routeClass(actor: ActorRef) = new AdminRoutes(actor)

  "AdminRoute" should "respond to faculty creation with success status" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case AddFaculty(id,_, _) => sender() ! SisdnCreated(id) }}))).route

    Post("/admin/faculties", Faculty("1", "fac1", None, None)) ~> adminRoute(user) ~> check{
      handled shouldBe true
      status shouldEqual StatusCodes.Created
    }
  }

  it should "respond to department creation with success status" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case AddDepartment(id,_, _) => sender() ! SisdnCreated(id) }}))).route

    Post("/admin/departments", Department("1", "fac1", "title", None, None)) ~> Route.seal(adminRoute(user)) ~> check{
      handled shouldBe true
      status shouldEqual StatusCodes.Created
    }
  }

  it should "respond to Correct faculty update with success" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case UpdateFaculty(id,_, _) => sender() ! SisdnUpdated(id) }}))).route

    Put("/admin/faculties", Faculty("1", "fac1", None, None)) ~> adminRoute(user) ~> check {
      handled shouldBe true
      status shouldEqual StatusCodes.OK
    }
  }

  it should "fail to create department with proper response" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case AddDepartment(_,_,_) =>
        sender() ! SisdnInvalid("validation", "errors") }}))).route

    Post("/admin/departments", Department("1", "dep1", "", None, None)) ~> adminRoute(user) ~> check{
      handled shouldBe true
      status shouldEqual StatusCodes.BadRequest
    }
  }

  it should "respond with forbidden for unauthorized action in courses" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case AddCourse(id,_,_) =>
        sender() ! SisdnUnauthorized(id) }}))).route

    Post("/admin/courses", Course("1", "", "", "crs", None, None, None)) ~> adminRoute(user) ~> check{
      handled shouldBe true
      status shouldEqual StatusCodes.Unauthorized
    }
  }

  it should "properly unmarshal BigDecimal value in formFields" in {
    val adminRoute = routeClass(system.actorOf(Props(creator = new Actor() {
      override def receive = { case AddProgram(id, _, prog) =>
        sender() ! SisdnCreated(id)
        }}))).route

    Post("/admin/programs", Program("id", "title", 8, 8.77 ,"program", None, Some("org"))) ~>
      adminRoute(user) ~> check{ handled shouldBe true }
  }
}