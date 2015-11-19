package sisdn.Admin

import akka.actor.{ActorSystem, Actor, ActorLogging, Props}
import akka.persistence.PersistentActor
import sisdn.common._

class Organization(id: String) extends PersistentActor with ActorLogging {
  import Organization._
  override def persistenceId: String = id

  var state = new State(context.system)

  override def receiveRecover: Receive = {
    case evt: OrganizationEvt => state.update(evt)
  }

  override def receiveCommand: Receive = {
    case AddFaculty(id, user, faculty) =>
      if(state.faculties.map(_.id).contains(faculty.id)) {
        sender() ! SisdnInvalid(id, "Duplicate faculty")
        log.info("Found duplicate faculty")
      }
      else
        persist(FacultyAdded(id, user.subject, faculty.copy())) { evt =>
          state.update(evt)
          sender() ! SisdnCreated(id)
          log.info(s"Added faculty ${evt.faculty.id}")
        }

    case AddDepartment(id, user, department) =>
      if(state.departments.map(_.id).contains(department.id)) {
        sender() ! SisdnInvalid(id, "Duplicate department")
        log.info("Found duplicate department")
      }
      else
        persist(DepartmentAdded(id, user.subject, department.copy())) { evt =>
          state.update(evt)
          sender() ! SisdnCreated(id)
          log.info(s"Added department ${evt.department.id}")
        }

    case AddCourse(id, user, course) =>
      if(state.courses.map(_.id).contains(course.id)) {
        sender() ! SisdnInvalid(id, "Duplicate course")
        log.info("Found duplicate course")
      }
      else
        persist(CourseAdded(id, user.subject, course.copy())) { evt =>
          state.update(evt)
          sender() ! SisdnCreated(id)
          log.info(s"Added course ${evt.course.id}")
        }

    case AddProgram(id, user, program) =>
      if(state.programs.map(_.id).contains(program.id)) {
        sender() ! SisdnInvalid(id, "duplicate program")
        log.info("Found duplicate program")
      }
      else
        persist(ProgramAdded(id, user.subject, program.copy())) { evt =>
          state.update(evt)
          sender() ! SisdnCreated(id)
          log.info(s"Added program ${evt.program.id}")
        }
  }
}

object Organization {
  def props(id: String) = Props(classOf[Organization], id)

  sealed trait OrgCmd extends ObjectWithId { val user: User; val entity: OrganizationEntity }
  case class AddFaculty(id: String, user: User, entity: Faculty) extends OrgCmd
  case class AddDepartment(id: String, user: User, entity: Department) extends OrgCmd
  case class AddCourse(id: String, user: User, entity: Course) extends OrgCmd
  case class AddProgram(id: String, user: User, entity: Program) extends OrgCmd

  sealed trait OrganizationEvt extends ObjectWithId
  case class FacultyAdded(id: String, user: String, faculty: Faculty) extends OrganizationEvt
  case class DepartmentAdded(id: String, user: String, department: Department) extends OrganizationEvt
  case class CourseAdded(id: String, user: String, course: Course) extends OrganizationEvt
  case class ProgramAdded(id: String, user: String, program: Program) extends OrganizationEvt

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

  sealed trait OrganizationEntity {val id: String
    val title: String
    val titleTr: Option[String]
    val org: String}

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
    facultyId: String,
    org: String,
    active: Option[Boolean] = Some(true)
  ) extends OrganizationEntity

  case class Course
  (
    id: String,
    title: String,
    titleTr: Option[String],
    departmentId: String,
    facultyId: String,
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
