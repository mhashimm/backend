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
        persist(AddedFaculty(f.user.subject, f.faculty.copy())) { state.update }

    case d: AddDepartment =>
      if(state.departments.map(_.id).contains(d.department.id))
      //TODO make your own Exceptions
        throw new Exception("Entity with this id exists")
      else
        persist(AddedDepartment(d.user.subject, d.department.copy())) { state.update }

    case c: AddCourse =>
      if(state.courses.map(_.id).contains(c.course.id))
      //TODO make your own Exceptions
        throw new Exception("Entity with this id exists")
      else
        persist(AddedCourse(c.user.subject, c.course.copy())) { state.update }

    case p: AddProgram =>
      if(state.programs.map(_.id).contains(p.program.id))
      //TODO make your own Exceptions
        throw new Exception("Entity with this id exists")
      else
        persist(AddedProgram(p.user.subject, p.program.copy())) { state.update }
  }
}

object Organization {
  def props(id: String) = Props(classOf[Organization], id)

  //commands
  case class AddFaculty(user: User, faculty: Faculty)
  case class AddDepartment(user: User, department: Department)
  case class AddCourse(user: User, course: Course)
  case class AddProgram(user: User, program: Program)

  //events
  sealed trait OrganizationEvt
  case class AddedFaculty(user: String, faculty: Faculty) extends OrganizationEvt
  case class AddedDepartment(user: String, department: Department) extends OrganizationEvt
  case class AddedCourse(user: String, course: Course) extends OrganizationEvt
  case class AddedProgram(user: String, program: Program) extends OrganizationEvt

  class State {
    def update(evt: OrganizationEvt): Unit = evt match {
        case f: AddedFaculty => faculties = faculties + f.faculty
        case d: AddedDepartment => departments = departments + d.department
        case c: AddedCourse => courses = courses + c.course
        case p: AddedProgram => programs = programs + p.program
        case _ => //TODO add logging
      }

    var faculties = Set[Faculty]()
    var departments = Set[Department]()
    var courses = Set[Course]()
    var programs = Set[Program]()
  }

  sealed trait OrganizationEntity

  case class Faculty
  (
    id: Int,
    name: String,
    nameTr: String,
    org: String
  ) extends OrganizationEntity

  case class Department
  (
    id: Int,
    name: String,
    nameTr: String,
    org: String
  ) extends OrganizationEntity

  case class Course
  (
    id: String,
    department: Int,
    remarks: String,
    title: String,
    titleTr: String,
    org: String
  ) extends OrganizationEntity

  case class Program
  (
    id: String,
    creditHours: BigDecimal,
    degree: String,
    degreeTr: String,
    facultyId: Int,
    terms: Int,
    org: String
  ) extends OrganizationEntity
}
