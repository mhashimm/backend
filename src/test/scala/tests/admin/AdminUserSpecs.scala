package tests.admin

import akka.actor.ActorSystem
import akka.testkit.{TestProbe, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpecLike}
import sisdn.Admin.AdminUser
import sisdn.Admin.Organization._
import sisdn.common.{SisdnUnauthorized, User}

class AdminUserSpecs(_system: ActorSystem) extends TestKit(_system) with FlatSpecLike
    with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("AdminUserSpec"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  val claims = Some(Set("admin_org1"))
  val dep = Department("dep1", "fac1", "", None, Some("org1"))
  val fac = Faculty("fac1", "", None, Some("org1"))
  val crs = Course("crs1", "dep1", "fac1", "", None, None, Some("org1"))
  val user = User("user1", "org1", Some(Set(dep.id)), Some(Set(fac.id)), claims)

  "AdminUser" should "have claim for faculty" in {
    val org, portal = TestProbe()
    val adminUser = system.actorOf(AdminUser.props("user1", org.ref))
    adminUser.tell(AddFaculty("id1", user, fac), portal.ref )
    org.expectMsg(AddFaculty("id1", user, fac))
  }

  it should "reject request with no claims" in {
    val org, portal = TestProbe()
    val adminUser = system.actorOf(AdminUser.props("user1", org.ref))
    adminUser.tell(AddFaculty("id1", user, fac.copy(org = Some("non-existing"))), portal.ref )
    portal.expectMsg(SisdnUnauthorized("id1"))
  }

  it should "accept request for department admin " in {
    val user1 = user.copy(claims = Some(Set("admin_fac1")))
    val org, portal = TestProbe()
    val adminUser = system.actorOf(AdminUser.props("user1", org.ref))
    adminUser.tell(AddDepartment("id1", user1, dep), portal.ref )
    org.expectMsg(AddDepartment("id1", user1, dep))
  }

  it should "accept request for faculty admin for department entity" in {
    val org, portal = TestProbe()
    val adminUser = system.actorOf(AdminUser.props("user1", org.ref))
    adminUser.tell(AddDepartment("id1", user, dep), portal.ref )
    org.expectMsg(AddDepartment("id1", user, dep))
  }

  it should "reject request from different org admin" in {
    val user1 = user.copy(claims = Some(Set("admin_fac1")))
    val org, portal = TestProbe()
    val adminUser = system.actorOf(AdminUser.props("user1", org.ref))
    adminUser.tell(AddDepartment("id1", user1.copy(org = "other-org"), dep), portal.ref )
    portal.expectMsg(SisdnUnauthorized("id1"))
  }

  it should "accept request for department admin for course entity" in {
    val user1 = user.copy(claims = Some(Set("admin_dep1")))
    val org, portal = TestProbe()
    val adminUser = system.actorOf(AdminUser.props("user1", org.ref))
    adminUser.tell(AddCourse("id1", user1, crs), portal.ref )
    org.expectMsg(AddCourse("id1", user1, crs))
  }

  it should "reject request for department admin of different department" in {
    val user1 = user.copy(claims = Some(Set("admin_dep2")))
    val org, portal = TestProbe()
    val adminUser = system.actorOf(AdminUser.props("user1", org.ref))
    adminUser.tell(AddCourse("id1", user1, crs), portal.ref )
    portal.expectMsg(SisdnUnauthorized("id1"))
  }

  it should "reject request for department admin of different organization" in {
    val user1 = user.copy(org = "org2", claims = Some(Set("admin_dep1")))
    val org, portal = TestProbe()
    val adminUser = system.actorOf(AdminUser.props("user1", org.ref))
    adminUser.tell(AddCourse("id1", user1, crs), portal.ref )
    portal.expectMsg(SisdnUnauthorized("id1"))
  }
}
