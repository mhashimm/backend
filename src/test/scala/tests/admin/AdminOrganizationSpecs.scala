package tests.admin

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpecLike}
import sisdn.Admin.Organization
import sisdn.Admin.Organization._
import sisdn.common._

class AdminOrganizationSpecs(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with FlatSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("AdmissionSpec"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  val faculty = Faculty("id1", "title1", None, None)
  val department = Department(uuid, "id1", "dep1", None, None)
  val user = User("name", "test-org", None, None, None)


  "AddFaculty" should "Accept valid entry" in {
    val adminOrg = system.actorOf(Organization.props("1"))
    adminOrg ! AddFaculty("uniq", user, faculty)
    expectMsg(SisdnCreated("uniq"))
  }

  it should "fail for duplicate faculty addition" in {
    val adminOrg = system.actorOf(Organization.props("2"))
    adminOrg ! AddFaculty("1", user, faculty)
    expectMsg(SisdnCreated("1"))
    adminOrg ! AddFaculty("1", user, faculty)
    expectMsg(SisdnInvalid("1", "Duplicate faculty"))
  }

  "UpdateFaculty" should "Successfuly update existing faculty" in {
    val adminOrg = system.actorOf(Organization.props("3"))
    adminOrg ! AddFaculty("1", user, faculty)
    expectMsg(SisdnCreated("1"))
    adminOrg ! UpdateFaculty("1", user, faculty)
    expectMsg(SisdnUpdated("1"))
  }

  it should "Fail update of nonexisting faculty" in {
    val adminOrg = system.actorOf(Organization.props("4"))
    adminOrg ! UpdateFaculty("1", user, faculty.copy(id = "non-existing"))
    expectMsg(SisdnNotFound("1"))
  }

  "Add Department" should "fail if added with non-existing faculty" in {
    val adminOrg = system.actorOf(Organization.props("4"))
    adminOrg ! AddDepartment("1", user, department)
    expectMsg(SisdnInvalid("1", "Faculty does not exist or is inactive"))
  }

  "Add Department" should "fail if added with inactive faculty" in {
    val adminOrg = system.actorOf(Organization.props("5"))
    adminOrg ! AddFaculty("1", user, faculty.copy(isActive = Some(false)))
    expectMsg(SisdnCreated("1"))
    adminOrg ! AddDepartment("1", user, department)
    expectMsg(SisdnInvalid("1", "Faculty does not exist or is inactive"))
  }
}
