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

  val user = User("subject", "org", None, None, Some(Set("admin_org")))

  def routeClass(actor: ActorRef) = new AdminRoutes(actor)

  "AdminRoute" should "respond to faculty creation with success status" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case AddFaculty(id,_, _) => sender() ! SisdnCreated(id) }}))).route

    Post("/admin/faculties", Faculty("1", "fac1", None, Some("org"))) ~> adminRoute(user) ~> check{
      //handled shouldBe true
      status shouldEqual StatusCodes.Created
    }
  }

  it should "respond to department creation with success status" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case AddDepartment(id,_, _) => sender() ! SisdnCreated(id) }}))).route

    Post("/admin/departments", Department("1", "fac1", "title", None, Some("org"))) ~>
      Route.seal(adminRoute(user)) ~> check{
        handled shouldBe true
        status shouldEqual StatusCodes.Created
    }
  }

  it should "respond to Correct faculty update with success" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case UpdateFaculty(id,_, _) => sender() ! SisdnUpdated(id) }}))).route

    Put("/admin/faculties", Faculty("1", "fac1", None, Some("org"))) ~> adminRoute(user) ~> check {
      handled shouldBe true
      status shouldEqual StatusCodes.OK
    }
  }

  it should "fail to create department with proper response" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case AddDepartment(_,_,_) =>
        sender() ! SisdnInvalid("validation", "errors") }}))).route

    Post("/admin/departments", Department("1", "dep1", "", None, Some("org"))) ~> adminRoute(user) ~> check{
      handled shouldBe true
      status shouldEqual StatusCodes.BadRequest
    }
  }

  it should "respond with forbidden for unauthorized action in courses" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case AddCourse(id,_,_) =>
        sender() ! SisdnUnauthorized(id) }}))).route

    Post("/admin/courses", Course("1", "", "", "crs", None, None, Some("org"))) ~> adminRoute(user) ~> check{
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

  it should "reject request from different org admin" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case UpdateFaculty(id,_, _) => sender() ! SisdnUpdated(id) }}))).route

    Put("/admin/faculties", Faculty("1", "fac1", None, Some("org"))) ~>
      Route.seal(adminRoute(user.copy(claims = Some(Set("admin_other-org"))))) ~> check {
      handled shouldBe true
      status shouldEqual StatusCodes.Forbidden
    }
  }

  it should "accept request for faculty admin to create department entity" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case UpdateDepartment(id,_, _) => sender() ! SisdnUpdated(id) }}))).route

    Put("/admin/departments", Department("dep1", "fac1", "", None, Some("org"))) ~>
      Route.seal(adminRoute(user.copy(claims = Some(Set("admin_fac1"))))) ~> check {
      handled shouldBe true
      status shouldEqual StatusCodes.OK
    }
  }

  it should "accept request for department admin to create course entity" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case AddCourse(id,_, _) => sender() ! SisdnUpdated(id) }}))).route

    Post("/admin/courses", Course("crs1", "dep1", "fac1", "", None, None, Some("org"))) ~>
      Route.seal(adminRoute(user.copy(claims = Some(Set("admin_dep1"))))) ~> check {
        handled shouldBe true
        status shouldEqual StatusCodes.OK
    }
  }

  it should "admin user should have claim for faculty" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case UpdateFaculty(id,_, _) => sender() ! SisdnUpdated(id) }}))).route

    Put("/admin/faculties", Faculty("1", "fac1", None, Some("org"))) ~>
      Route.seal(adminRoute(user.copy(claims = Some(Set("admin_org"))))) ~> check {
      handled shouldBe true
      status shouldEqual StatusCodes.OK
    }
  }

  it should "reject request with no claims" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case UpdateFaculty(id,_, _) => sender() ! SisdnUpdated(id) }}))).route

    Put("/admin/faculties", Faculty("1", "fac1", None, Some("non-existing"))) ~>
      Route.seal(adminRoute(user.copy(claims = Some(Set("admin_org"))))) ~> check {
      handled shouldBe true
      status shouldEqual StatusCodes.Forbidden
    }
  }

  it should "accept request for department admin" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case UpdateDepartment(id,_, _) => sender() ! SisdnUpdated(id) }}))).route

    Put("/admin/departments", Department("dep1", "fac1", "", None, Some("org"))) ~>
      Route.seal(adminRoute(user.copy(claims = Some(Set("admin_fac1"))))) ~> check {
      handled shouldBe true
      status shouldEqual StatusCodes.OK
    }
  }

  it should "reject request for department admin of different department" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case AddCourse(id,_, _) => sender() ! SisdnUpdated(id) }}))).route

    Post("/admin/courses", Course("crs1", "dep1", "fac1", "", None, None, Some("org"))) ~>
      Route.seal(adminRoute(user.copy(claims = Some(Set("admin_dep2"))))) ~> check {
      handled shouldBe true
      status shouldEqual StatusCodes.Forbidden
    }
  }

  it should "reject request for department admin of different organization" in {
    val adminRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive = { case AddCourse(id,_, _) => sender() ! SisdnUpdated(id) }}))).route

    Post("/admin/courses", Course("crs1", "dep1", "fac1", "", None, None, Some("other-org"))) ~>
      Route.seal(adminRoute(user.copy(claims = Some(Set("admin_dep1"))))) ~> check {
      handled shouldBe true
      status shouldEqual StatusCodes.Forbidden
    }
  }
}