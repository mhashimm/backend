package tests.admin

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpecLike}
import sisdn.admin.Organization
import sisdn.admin.Organization._
import sisdn.common._

class AdminOrganizationSpecs(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with FlatSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("AdminOrganizationSpecs"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  val faculty = Faculty(uuid, "title1", None, Some(uuid))
  val department = Department(uuid, "id1", "dep1", None,  Some(uuid))
  val user = User("name", "test-org", None, None, None)


  "AddFaculty" should "Accept valid entry" in {
    val adminOrg = system.actorOf(Organization.props("org"))
    adminOrg ! AddFaculty("uniq", user, faculty)
    expectMsg(SisdnCreated("uniq"))
  }

  it should "fail for duplicate faculty addition" in {
    val adminOrg = system.actorOf(Organization.props("org"))
    val fac = faculty.copy(id = uuid)
    adminOrg ! AddFaculty("1", user, fac)
    expectMsg(SisdnCreated("1"))
    adminOrg ! AddFaculty("1", user, fac)
    expectMsg(SisdnInvalid("1", "Duplicate faculty"))
  }

  "UpdateFaculty" should "Successfully update existing faculty" in {
    val adminOrg = system.actorOf(Organization.props("org"))
    val fac = faculty.copy(id = uuid)
    adminOrg ! AddFaculty("1", user, fac)
    expectMsg(SisdnCreated("1"))
    adminOrg ! UpdateFaculty("1", user, fac)
    expectMsg(SisdnUpdated("1"))
  }

  it should "Fail update of non-existing faculty" in {
    val adminOrg = system.actorOf(Organization.props("org"))
    adminOrg ! UpdateFaculty("1", user, faculty.copy(id = "non-existing"))
    expectMsg(SisdnNotFound("1"))
  }

  "Add Department" should "fail if added with non-existing faculty" in {
    val adminOrg = system.actorOf(Organization.props("org"))
    adminOrg ! AddDepartment("1", user, department)
    expectMsg(SisdnInvalid("1", "Faculty does not exist or is inactive"))
  }

  it should "fail if added with inactive faculty" in {
    val adminOrg = system.actorOf(Organization.props("org"))
    val uid = uuid
    adminOrg ! AddFaculty("1", user, faculty.copy(id = uid, isActive = Some(false)))
    expectMsg(SisdnCreated("1"))
    adminOrg ! AddDepartment("1", user, department.copy( facultyId = uid))
    expectMsg(SisdnInvalid("1", "Faculty does not exist or is inactive"))
  }

  "Organization state" should "correctly add department to state" in {
    val state = new State(system)
    state.update(DepartmentAdded("", "", department,0))

    state.departments should contain (department)
  }

  it should "correctly update faculty in state" in {
    val state = new State(system)
    state.update(DepartmentAdded("", "", department,0))
    state.update(DepartmentUpdated("", "", department.copy(titleTr = Some("test")),0))
    val result = state.departments.find(_.id == department.id ).get.titleTr
    result shouldEqual Some("test")
    state.departments.size shouldEqual 1
  }

  it should "Positively validate a list of orgEntities" in {
    val adminOrg = system.actorOf(Organization.props("org" + uuid))
    adminOrg ! AddFaculty("1", user, faculty)
    expectMsg(SisdnCreated("1"))
    adminOrg ! AddDepartment("1", user, department.copy(facultyId = faculty.id))
    expectMsg(SisdnCreated("1"))

    adminOrg ! OrgValidCmd(List(department, faculty))

    expectMsg(true)
  }

  it should "Negatively validate a list of orgEntities for non-existing Entities" in {
    val adminOrg = system.actorOf(Organization.props("org" + uuid))
    val uid = uuid
    adminOrg ! AddFaculty("1", user, faculty.copy(id = uid, isActive = Some(true)))
    expectMsg(SisdnCreated("1"))

    adminOrg ! OrgValidCmd(List(faculty.copy(id = uuid, isActive = Some(true))))

    expectMsg(false)
  }

  it should "Negatively validate a list of orgEntities for org with no entities" in {
    val adminOrg = system.actorOf(Organization.props("org" + uuid))

    adminOrg ! OrgValidCmd(List(faculty))

    expectMsg(false)
  }

  it should "Negatively validate a list of orgEntities for inactive Entities" in {
    val adminOrg = system.actorOf(Organization.props("org" + uuid))
    val uid = uuid
    val f = faculty.copy(id = uid, isActive = Some(false))
    adminOrg ! AddFaculty("1", user, f)
    expectMsg(SisdnCreated("1"))

    adminOrg ! OrgValidCmd(List(f))

    expectMsg(false)
  }
}
