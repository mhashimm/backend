package sisdn.admin

import akka.actor.ActorSystem
import akka.persistence.query.EventEnvelope
import akka.stream.ActorMaterializer
import scala.concurrent.{Future, ExecutionContextExecutor}
//import slick.driver.H2Driver.api._
import slick.driver.PostgresDriver.api._

import sisdn.admin.Organization._

trait AdminQuery {
  implicit val system: ActorSystem
  implicit val executor: ExecutionContextExecutor
  implicit val materializer: ActorMaterializer
  val db = Database.forConfig("postgres")
  val streamOffsets = TableQuery[StreamOffsets]
  val faculties = TableQuery[Faculties]
  val departments = TableQuery[Departments]
  val courses = TableQuery[Courses]
  val programs = TableQuery[Programs]

  def writeToDB(event: EventEnvelope): Future[Unit] = event.event match {
    case e:FacultyAdded  => db.run( DBIO.seq(
      //TODO due to a bug in postgres driver we cant insert row with composite PK so ...
      faculties.insertOrUpdate(FacultyRow(e.faculty.id, e.faculty.title, e.faculty.titleTr,
        e.faculty.org.get, e.faculty.isActive.get, e.timestamp)),
      streamOffsets.update("organization", event.sequenceNr)))
    case e:FacultyUpdated => db.run( DBIO.seq(
      faculties.insertOrUpdate(FacultyRow(e.faculty.id, e.faculty.title, e.faculty.titleTr,
        e.faculty.org.get, e.faculty.isActive.get, e.timestamp)),
      streamOffsets.update("organization", event.sequenceNr)))
    case e:DepartmentAdded => db.run(DBIO.seq(
      departments.insertOrUpdate(DepartmentRow(e.department.id, e.department.facultyId, e.department.title,
        e.department.titleTr, e.department.org.get, e.department.isActive.get, e.timestamp)),
      streamOffsets.update("organization", event.sequenceNr)))
    case e:DepartmentUpdated => db.run(DBIO.seq(
      departments.insertOrUpdate(DepartmentRow(e.department.id, e.department.facultyId, e.department.title,
        e.department.titleTr, e.department.org.get, e.department.isActive.get, e.timestamp)),
      streamOffsets.update("organization", event.sequenceNr)))
    case e: CourseAdded => db.run(DBIO.seq(
      courses.insertOrUpdate(CourseRow(e.course.id, e.course.departmentId, e.course.facultyId, e.course.title,
        e.course.titleTr, e.course.org.get, e.course.isActive.get, e.course.remarks, e.timestamp)),
      streamOffsets.update("organization", event.sequenceNr)))
    case e: CourseUpdated => db.run(DBIO.seq(
      courses.insertOrUpdate(CourseRow(e.course.id, e.course.departmentId, e.course.facultyId, e.course.title,
        e.course.titleTr, e.course.org.get, e.course.isActive.get, e.course.remarks, e.timestamp)),
      streamOffsets.update("organization", event.sequenceNr)))
    case e:ProgramAdded => db.run(DBIO.seq(
      programs.insertOrUpdate(ProgramRow(e.program.id, e.program.facultyId, e.program.terms, e.program.creditHours,
         e.program.title, e.program.titleTr, e.program.org.get, e.program.isActive.get, e.timestamp)),
      streamOffsets.update("organization", event.sequenceNr)))
    case e:ProgramUpdated => db.run(DBIO.seq(
      programs.insertOrUpdate(ProgramRow(e.program.id, e.program.facultyId, e.program.terms, e.program.creditHours,
        e.program.title, e.program.titleTr, e.program.org.get, e.program.isActive.get, e.timestamp)),
      streamOffsets.update("organization", event.sequenceNr)))
    case _ => Future.successful(())
  }
}

class Programs(tag: Tag) extends Table[ProgramRow](tag, "programs"){
  def id = column[String]("id", O.PrimaryKey)
  def facultyId = column[String]("facultyId")
  def terms = column[Int]("terms")
  def creditHours = column[BigDecimal]("creditHours")
  def title = column[String]("title")
  def titleTr = column[Option[String]]("titleTr")
  def org = column[String]("org", O.PrimaryKey)
  def isActive = column[Boolean]("isActive")
  def ts = column[Long]("ts")

  def * = (id, facultyId, terms, creditHours, title, titleTr, org, isActive, ts) <> (ProgramRow.tupled, ProgramRow.unapply)
  //def pk = primaryKey("programs_pk", (id, org))
}

class Courses(tag: Tag) extends Table[CourseRow](tag, "courses"){
  def id = column[String]("id", O.PrimaryKey)
  def departmentId = column[String]("departmentId")
  def facultyId = column[String]("facultyId")
  def title = column[String]("title")
  def titleTr = column[Option[String]]("titleTr")
  def org = column[String]("org", O.PrimaryKey)
  def isActive = column[Boolean]("isActive")
  def remarks = column[Option[String]]("remarks")
  def ts = column[Long]("ts")

  def * = (id, departmentId, facultyId, title, titleTr, org, isActive, remarks, ts) <> (CourseRow.tupled, CourseRow.unapply)
  //def pk = primaryKey("courses_pk", (id, org))
}

class Departments(tag: Tag) extends Table[DepartmentRow](tag, "departments"){
  def id = column[String]("id", O.PrimaryKey)
  def facultyId = column[String]("facultyId")
  def title = column[String]("title")
  def titleTr = column[Option[String]]("titleTr")
  def org = column[String]("org", O.PrimaryKey)
  def isActive = column[Boolean]("isActive")
  def ts = column[Long]("ts")

  def * = (id, facultyId, title, titleTr, org, isActive, ts) <> (DepartmentRow.tupled, DepartmentRow.unapply)
  //def pk = primaryKey("departments_pk", (id, org))
}

class Faculties(tag: Tag) extends Table[FacultyRow](tag, "faculties") {
  def id = column[String]("id", O.PrimaryKey)
  def title = column[String]("title")
  def titleTr = column[Option[String]]("titleTr")
  def org = column[String]("org", O.PrimaryKey)
  def isActive = column[Boolean]("isActive")
  def ts = column[Long]("ts")

  def * = (id, title, titleTr, org, isActive, ts) <> (FacultyRow.tupled, FacultyRow.unapply)
  //def pk = primaryKey("faculties_pk", (id, org))
}

class StreamOffsets(tag: Tag) extends Table[(String, Long)](tag, "streamOffsets"){
  def id = column[String]("id", O.PrimaryKey)
  def offset = column[Long]("offset")

  def * = (id, offset)
}



case class CourseRow(id: String, departmentId: String, facultyId: String, title: String,
  titleTr: Option[String], org: String, isActive: Boolean, remarks: Option[String], ts: Long)

case class DepartmentRow(id: String, facultyId: String, title: String, titleTr:
  Option[String], org: String, isActive: Boolean, ts:Long)

case class FacultyRow(id: String, title: String, titleTr: Option[String], org: String,
   isActive: Boolean, ts: Long)

case class ProgramRow(id: String, facultyId: String, terms: Int, creditHours: BigDecimal,
   title: String, titleTr: Option[String], org: String, isActive: Boolean, ts: Long)