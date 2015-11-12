package sisdn.Admin

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor
import sisdn.common.User

class Organization(id: String) extends PersistentActor with ActorLogging {
  import Organization._
  override def persistenceId: String = id

  var state = new State

  override def receiveRecover: Receive = {
    case evt: OrganizationEvt => state.update(evt)
  }

  override def receiveCommand: Receive = {
    case f: AddFaculty =>
      if(state.faculties.map(_.id).contains(f.faculty.id))
        //TODO make your own Exceptions
        throw new Exception("Entity with this id exists")
      else
        persist(FacultyAdded(f.user.subject, f.faculty.copy())) { state.update }

    case d: AddDepartment =>
      if(state.departments.map(_.id).contains(d.department.id))
      //TODO make your own Exceptions
        throw new Exception("Entity with this id exists")
      else
        persist(DepartmentAdded(d.user.subject, d.department.copy())) { state.update }

    case c: AddCourse =>
      if(state.courses.map(_.id).contains(c.course.id))
      //TODO make your own Exceptions
        throw new Exception("Entity with this id exists")
      else
        persist(CourseAdded(c.user.subject, c.course.copy())) { state.update }

    case p: AddProgram =>
      if(state.programs.map(_.id).contains(p.program.id))
      //TODO make your own Exceptions
        throw new Exception("Entity with this id exists")
      else
        persist(ProgramAdded(p.user.subject, p.program.copy())) { state.update }
  }
}

object Organization {
  def props(id: String) = Props(classOf[Organization], id)

  sealed trait OrgCmd { val user: User}
  case class AddFaculty(user: User, faculty: Faculty) extends OrgCmd
  case class AddDepartment(user: User, department: Department) extends OrgCmd
  case class AddCourse(user: User, course: Course) extends OrgCmd
  case class AddProgram(user: User, program: Program) extends OrgCmd

  sealed trait OrganizationEvt
  case class FacultyAdded(user: String, faculty: Faculty) extends OrganizationEvt
  case class DepartmentAdded(user: String, department: Department) extends OrganizationEvt
  case class CourseAdded(user: String, course: Course) extends OrganizationEvt
  case class ProgramAdded(user: String, program: Program) extends OrganizationEvt

  class State {
    def update(evt: OrganizationEvt): Unit = evt match {
        case f: FacultyAdded => faculties = faculties + f.faculty
        case d: DepartmentAdded => departments = departments + d.department
        case c: CourseAdded => courses = courses + c.course
        case p: ProgramAdded => programs = programs + p.program
        case _ => //TODO add logging
      }

    var faculties   = Set[Faculty]()
    var departments = Set[Department]()
    var courses     = Set[Course]()
    var programs    = Set[Program]()
  }

  sealed trait OrganizationEntity

  case class Faculty
  (
    id: String,
    title: String,
    titleTr: Option[String],
    org: String
  ) extends OrganizationEntity

  case class Department
  (
    id: String,
    title: String,
    titleTr: Option[String],
    org: String
  ) extends OrganizationEntity

  case class Course
  (
    id: String,
    department: String,
    remarks: Option[String],
    title: String,
    titleTr: Option[String],
    org: String
  ) extends OrganizationEntity

  case class Program
  (
    id: String,
    creditHours: BigDecimal,
    title: String,
    titleTr: Option[String],
    facultyId: String,
    terms: Int,
    org: String
  ) extends OrganizationEntity
}
