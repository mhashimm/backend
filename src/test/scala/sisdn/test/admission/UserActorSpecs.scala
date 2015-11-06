package sisdn.admission.test

import akka.actor
import akka.actor.{Props, ActorSystem}
import akka.testkit._
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpecLike}
import sisdn.admission.model.{Student, User}
import sisdn.admission.service.AdmissionUser
import sisdn.admission.service.AdmissionUser.{AdmissionStatusUpdateEvt, Admit}
import scala.concurrent.duration.DurationInt

class UserActorSpecs (_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with FlatSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("UserActorSpec"))
  override def afterAll { TestKit.shutdownActorSystem(system) }
  implicit val ec = system.dispatcher

  val admitor = TestProbe()

  val user = system.actorOf(AdmissionUser.props("1", admitor.ref))
  val students = List.range(1,4).flatMap{ x => List(Student(x.toHexString,"",x,x,"org")) }

  "UserActor" should "extract correct number of admission to list" in {
    user ! Admit(User("1","",None,None), students)
    val admissions = admitor.receiveWhile(){ case ad: AdmissionStatusUpdateEvt => ad}
    admissions.length shouldEqual 3
  }

  it should "set the status to Pending for new admissions" in {

    fail("Not implemented")
  }

  it should "set the status to Valid for validated admissions" in {

    fail("Not implemented")
  }

  it should "set the status to Rejected for rejected admissions" in {

    fail("Not implemented")
  }

  it should "set the status to Accepted for accepted admissions" in {

    fail("Not implemented")
  }

  it should "should not respond to duplicate admissions" in {

    fail("Not implemented")
  }
}
