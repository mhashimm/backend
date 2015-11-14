package sisdn.Admin

import akka.actor.{ActorSystem, Actor, ActorLogging, Props}
import akka.persistence.PersistentActor
import sisdn.common.{SisdnInvalid, SisdnCreated, User, DuplicateEntityException}

class Organization(id: String) extends PersistentActor with ActorLogging {
  import Organization._
  override def persistenceId: String = id

  var state = new State(context.system)

  override def receiveRecover: Receive = {
    case evt: OrganizationEvt => state.update(evt)
  }

  override def receiveCommand: Receive = {
    case f: AddFaculty =>
      if(state.faculties.map(_.id).contains(f.faculty.id)) {
        sender() ! SisdnInvalid("Duplicate faculty")
        log.info("Found duplicate faculty")
      }
      else
        persist(FacultyAdded(f.user.subject, f.faculty.copy())) { evt =>
          state.update(evt)
          sender() ! SisdnCreated
          log.info(s"Added faculty ${evt.faculty.id}")
        }

    case d: AddDepartment =>
      if(state.departments.map(_.id).contains(d.department.id)) {
        sender() ! SisdnInvalid("Duplicate department")
        log.info("Found duplicate department")
      }
      else
        persist(DepartmentAdded(d.user.subject, d.department.copy())) { evt =>
          state.update(evt)
          sender() ! SisdnCreated
          log.info(s"Added department ${evt.department.id}")
        }

    case c: AddCourse =>
      if(state.courses.map(_.id).contains(c.course.id)) {
        sender() ! SisdnInvalid("Duplicate course")
        log.info("Found duplicate course")
      }
      else
        persist(CourseAdded(c.user.subject, c.course.copy())) { evt =>
          state.update(evt)
          sender() ! SisdnCreated
          log.info(s"Added course ${evt.course.id}")
        }

    case p: AddProgram =>
      if(state.programs.map(_.id).contains(p.program.id)) {
        sender() ! SisdnInvalid("duplicate program")
        log.info("Found duplicate program")
      }
      else
        persist(ProgramAdded(p.user.subject, p.program.copy())) { evt =>
          state.update(evt)
          sender() ! SisdnCreated
          log.info(s"Added program ${evt.program.id}")
        }
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

  class State(system: ActorSystem) {
    def update(evt: OrganizationEvt): Unit = evt match {
        case f: FacultyAdded    => faculties = faculties + f.faculty
        case d: DepartmentAdded => departments = departments + d.department
        case c: CourseAdded     => courses = courses + c.course
        case p: ProgramAdded    => programs = programs + p.program
        case _                  =>
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
    org: String,
    active: Option[Boolean] = Some(true)
  ) extends OrganizationEntity

  case class Department
  (
    id: String,
    title: String,
    titleTr: Option[String],
    org: String,
    active: Option[Boolean] = Some(true)
  ) extends OrganizationEntity

  case class Course
  (
    id: String,
    title: String,
    titleTr: Option[String],
    departmentId: String,
    remarks: Option[String],
    org: String,
    active: Option[Boolean] = Some(true)
  ) extends OrganizationEntity

  case class Program
  (
    id: String,
    title: String,
    titleTr: Option[String],
    facultyId: String,
    terms: Int,
    creditHours: BigDecimal,
    org: String,
    active: Option[Boolean] = Some(true)
  ) extends OrganizationEntity
}
