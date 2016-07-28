package sisdn.admin

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import sisdn.common._

import scala.annotation.tailrec

class Organization(id: String) extends PersistentActor with ActorLogging {

  import Organization._

  override def persistenceId: String = id

  var state = new State(context.system)

  override def receiveRecover: Receive = {
    case evt: OrganizationEvt => state.update(evt)
  }

  override def receiveCommand: Receive = {
    case OrgValidCmd(entities) => sender() ! validateEntities(entities)

    case AddFaculty(id, user, faculty) =>
      if (state.faculties.map(_.id).contains(faculty.id)) {
        sender() ! SisdnInvalid(id, "Duplicate faculty")
        log.info(s"Found duplicate faculty ${faculty.id}")
      }
      else
        persist(FacultyAdded(id, user.username, faculty.copy(), System.currentTimeMillis())) { evt =>
          state.update(evt)
          sender() ! SisdnCreated(id)
          log.info(s"Added faculty ${evt.faculty.id}")
        }

    case UpdateFaculty(id, user, faculty) => state.faculties.find(_.id == faculty.id) match {
      case None => sender() ! SisdnNotFound(id)
        log.info(s"Faculty not found ${faculty.id}")
      case Some(found) => persist(FacultyUpdated(id, user.username, faculty.copy(id = found.id, org = found.org),
          System.currentTimeMillis())) { evt =>
        state.update(evt)
        sender() ! SisdnUpdated(id)
        log.info(s"updated faculty ${evt.faculty.id}")
      }
    }

    case AddDepartment(id, user, department) => state.departments.find(_.id == department.id) match {
      case None => state.faculties.find(_.id == department.facultyId) match {
        case Some(f) if f.isActive.get => persist(DepartmentAdded(id, user.username, department.copy(),
            System.currentTimeMillis())) { evt =>
          state.update(evt)
          sender() ! SisdnCreated(id)
          log.info(s"Added department ${evt.department.id}")
        }
        case _ => sender() ! SisdnInvalid(id, "Faculty does not exist or is inactive")
          log.info(s"Attempting to add department with non-existing/inactive faculty $id")
      }
      case Some(d) => sender() ! SisdnInvalid(id, "Duplicate department")
        log.info(s"Found duplicate department ${d.id}")
    }

    case UpdateDepartment(id, user, department) => state.departments.find(_.id == department.id) match {
      case None => sender() ! SisdnNotFound(id)
        log.info(s"Department not found ${department.id}")
      case Some(found) => state.faculties.find(_.id == department.facultyId) match {
        case Some(f) if f.isActive.get => persist(DepartmentUpdated(id, user.username,
          department.copy(id = found.id, org = found.org), System.currentTimeMillis())) { evt =>
          state.update(evt)
          sender() ! SisdnUpdated(id)
          log.info(s"updated department ${evt.department.id}")
        }
        case _ =>
          sender() ! SisdnInvalid(id, "Faculty does not exist or is inactive")
          log.info(s"Attempting to add department with non-existing/inactive faculty $id")
      }
    }

    case AddCourse(id, user, course) => state.courses.find(_.id == course.id) match {
      case None => (state.faculties.find(_.id == course.facultyId), state.departments.find(_.id == course.departmentId)) match {
        case (Some(f), Some(d)) if f.isActive.get && d.isActive.get =>
          persist(CourseAdded(id, user.username, course.copy(), System.currentTimeMillis())) { evt =>
          state.update(evt)
          sender() ! SisdnCreated(id)
          log.info(s"Added course ${evt.course.id}")
        }
        case _ => sender() ! SisdnInvalid(id, "Faculty/Department don't exist or are inactive")
          log.info(s"Attempting to add course with non-existing/inactive faculty/department $id")
      }
      case Some(c) => sender() ! SisdnInvalid(id, "Duplicate course")
        log.info("Found duplicate course")
    }

    case UpdateCourse(id, user, course) => state.courses.find(_.id == course.id) match {
      case None => sender() ! SisdnNotFound(id)
        log.info(s"course not found ${course.id}")
      case Some(found) => (state.faculties.find(_.id == course.facultyId), state.departments.find(_.id == course.departmentId)) match {
        case (Some(f), Some(d)) if d.isActive.get && f.isActive.get =>
          persist(CourseUpdated(id, user.username, course.copy(id = found.id, org = found.org), System.currentTimeMillis())) {
            evt =>
              state.update(evt)
              sender() ! SisdnUpdated(id)
              log.info(s"updated course ${evt.course.id}")
          }
        case _ => sender() ! SisdnInvalid(id, "Faculty/Department don't exist or are inactive")
          log.info(s"Attempting to update course with non-existing/inactive faculty/department $id")
      }
    }

    case AddProgram(id, user, program) => state.programs.find(_.id == program.id) match {
      case Some(p) => sender() ! SisdnInvalid(id, "duplicate program")
        log.info("Found duplicate program")
      case None => state.faculties.find(_.id == program.facultyId) match {
        case Some(f) if f.isActive.get => persist(ProgramAdded(id, user.username, program.copy(), System.currentTimeMillis())) { evt =>
          state.update(evt)
          sender() ! SisdnCreated(id)
          log.info(s"Added program ${evt.program.id}")
        }
        case _ => sender() ! SisdnInvalid(id, "Faculty don't exist or is inactive")
          log.info(s"Attempting to add program with non-existing/inactive faculty $id")
      }
    }

    case UpdateProgram(id, user, program) => state.programs.find(_.id == program.id) match {
      case None => sender() ! SisdnNotFound(id)
        log.info(s"Program not found ${program.id}")
      case Some(found) => state.faculties.find(_.id == program.facultyId) match {
        case Some(f) if f.isActive.get => persist(ProgramUpdated(id, user.username, program.copy(id = found.id, org = found.org),
            System.currentTimeMillis())) { evt =>
          state.update(evt)
          sender() ! SisdnUpdated(id)
          log.info(s"updated program ${evt.program.id}")
        }
        case _ => sender() ! SisdnInvalid(id, "Faculty don't exist or is inactive")
          log.info(s"Attempting to update program with non-existing/inactive faculty $id")
      }
    }
  }

  def validateEntities(entities: List[OrgEntity]): Boolean = {
    @tailrec
    def recurse(list: List[OrgEntity]): Boolean = list match {
      case _ if list.isEmpty => true
      case x :: xs => entityIsValid(x) && recurse(xs)
    }
    if (entities.isEmpty) false
    else recurse(entities)
  }

  def entityIsValid(entity: OrgEntity) : Boolean = entity match {
    case e: Faculty => state.faculties.find(p => p.id == entity.id).exists(_.isActive.get)
    case e: Department => state.departments.find(p => p.id == entity.id).exists(_.isActive.get)
    case e: Course => state.courses.find(p => p.id == entity.id).exists(_.isActive.get)
    case e: Program => state.programs.find(p => p.id == entity.id).exists(_.isActive.get)
  }
}

object Organization {
  def props(id: String) = Props(classOf[Organization], id)

  sealed trait OrgCmd extends ObjectWithId {
    val user: User;
    val entity: OrgEntity
  }

  case class AddFaculty(id: String, user: User, entity: Faculty) extends OrgCmd

  case class UpdateFaculty(id: String, user: User, entity: Faculty) extends OrgCmd

  case class AddDepartment(id: String, user: User, entity: Department) extends OrgCmd

  case class UpdateDepartment(id: String, user: User, entity: Department) extends OrgCmd

  case class AddCourse(id: String, user: User, entity: Course) extends OrgCmd

  case class UpdateCourse(id: String, user: User, entity: Course) extends OrgCmd

  case class AddProgram(id: String, user: User, entity: Program) extends OrgCmd

  case class UpdateProgram(id: String, user: User, entity: Program) extends OrgCmd

  case class GetEntities(courses: Option[Long], departments: Option[Long], faculties: Option[Long], programs: Option[Long])

  sealed trait OrganizationEvt extends ObjectWithId

  case class FacultyAdded(id: String, user: String, faculty: Faculty, timestamp: Long) extends OrganizationEvt

  case class FacultyUpdated(id: String, user: String, faculty: Faculty, timestamp: Long) extends OrganizationEvt

  case class DepartmentAdded(id: String, user: String, department: Department, timestamp: Long) extends OrganizationEvt

  case class DepartmentUpdated(id: String, user: String, department: Department, timestamp: Long) extends OrganizationEvt

  case class CourseAdded(id: String, user: String, course: Course, timestamp: Long) extends OrganizationEvt

  case class CourseUpdated(id: String, user: String, course: Course, timestamp: Long) extends OrganizationEvt

  case class ProgramAdded(id: String, user: String, program: Program, timestamp: Long) extends OrganizationEvt

  case class ProgramUpdated(id: String, user: String, program: Program, timestamp: Long) extends OrganizationEvt

  case class FoundEntities(courses: List[Course], departments: List[Department], faculties: List[Faculty], programs: List[Program])

  class State(system: ActorSystem) {
    def update(evt: OrganizationEvt): Unit = evt match {
      case f: FacultyAdded => faculties = faculties + f.faculty
      case f: FacultyUpdated => faculties = faculties.filterNot(_.id == f.faculty.id) + f.faculty
      case d: DepartmentAdded => departments = departments + d.department
      case d: DepartmentUpdated => departments = departments.filterNot(_.id == d.department.id) + d.department
      case c: CourseAdded => courses = courses + c.course
      case c: CourseUpdated => courses = courses.filterNot(_.id == c.course.id) + c.course
      case p: ProgramAdded => programs = programs + p.program
      case p: ProgramUpdated => programs = programs.filterNot(_.id == p.program.id) + p.program
      case _ =>
    }

    var faculties = Set[Faculty]()
    var departments = Set[Department]()
    var courses = Set[Course]()
    var programs = Set[Program]()
  }

}