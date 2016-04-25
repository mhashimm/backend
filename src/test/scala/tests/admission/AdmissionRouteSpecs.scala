package tests.admission

import sisdn.admission.AdmissionRoute
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtHeader}
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}
import MediaTypes._
import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import headers._
import sisdn.admission.AdmissionUser.Admission
import sisdn.common.{SisdnCreated, SisdnDuplicate, User}

import scala.concurrent.duration._
import scala.language.postfixOps

class AdmissionRouteSpecs extends FlatSpec with Matchers with ScalatestRouteTest {
  import AdmissionRouteSpecs._
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val timeout: Timeout = 3 second

  def routeClass(actor: ActorRef) = new AdmissionRoute(actor)

  "Admission Service" should "Return Success for POST Request" in {
    val admissionRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive: Receive = {
        case Admission(uuid, _, _) => sender() ! SisdnCreated(uuid)
      }
    }))).route(User("subject", "org", None, None, Some(Set("admitter_org"))))

    Post("/admit", HttpEntity(`application/json`, stdJson)).addHeader(hdr) ~> admissionRoute ~> check {
      status shouldBe StatusCodes.Created
    }
  }

  it should "Fail for duplicate admission with proper response" in {
    val admissionRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive: Receive = {
        case Admission(id, _, _) => sender() ! SisdnDuplicate(id)
      }
    }))).route(User("subject", "org", None, None, Some(Set("admitter_org"))))

    Post("/admit", HttpEntity(`application/json`, stdJson)).addHeader(hdr) ~> admissionRoute ~> check {
      status shouldBe StatusCodes.custom(409, "Duplicate admission")
    }
  }

  it should "Return MethodNotAllowed for GET Request" in {
    val admissionRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive: Receive = {
        case Admission(uuid, _, _) => sender() ! SisdnCreated(uuid)
      }
    }))).route(User("subject", "org", None, None, None))

    Get("/admit") ~> admissionRoute ~> check {
      rejection shouldEqual MethodRejection(HttpMethods.POST)
    }
  }

  it should """Accept request for /v1 route as the default route""" in {
    val admissionRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive: Receive = {
        case Admission(uuid, _, _) => sender() ! SisdnCreated(uuid)
      }
    }))).route(User("subject", "org", None, None, Some(Set("admitter_org"))))

    Post("/admit/v1", HttpEntity(`application/json`, stdJson)).addHeader(hdr) ~> admissionRoute ~> check {
      status shouldBe StatusCodes.Created
    }
  }

  it should """Fail for arbitrary Url""" in {
    val admissionRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive: Receive = {
        case Admission(uuid, _, _) => sender() ! SisdnCreated(uuid)
      }
    }))).route(User("subject", "org", None, None, None))

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

  it should "prohibit admitter from different org to admit" in {
    val admissionRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive: Receive = {
        case Admission(id, _, _) => sender() ! SisdnDuplicate(id)
      }
    }))).route(User("subject", "org", None, None, Some(Set("admitter_other-org"))))

    Post("/admit", HttpEntity(`application/json`, stdJson)).addHeader(hdr) ~>
      Route.seal(admissionRoute) ~> check {
      status shouldBe StatusCodes.Forbidden
    }
  }


  it should "prohibit user from the same org with no admitter privilege for faculty" in {
    val admissionRoute = routeClass(system.actorOf(Props(new Actor(){
      override def receive: Receive = {
        case Admission(id, _, _) => sender() ! SisdnDuplicate(id)
      }
    }))).route(User("subject", "org", None, None, Some(Set("admitter_2"))))

    Post("/admit", HttpEntity(`application/json`, stdJson)).addHeader(hdr) ~>
      Route.seal(admissionRoute) ~> check {
      status shouldBe StatusCodes.Forbidden
    }
  }
}

object AdmissionRouteSpecs{
  val config = ConfigFactory.load()
  val key = config.getString("sisdn.key")
  val stdJson =
    """{"id" : "1", "name" : "name", "thirdName" : "third",
      | "org" : "org", "faculty" : "1", "program" : "1"}""".stripMargin
  val claimsSet = JwtClaimsSet("""{"departments" : [1], "subject" : "subject",
                                 |"org" : "org", "faculties" : [1]}""".stripMargin)
  val jwt: String = JsonWebToken(JwtHeader("HS256"), claimsSet, key)

  val hdr = Authorization(OAuth2BearerToken(jwt))
}

