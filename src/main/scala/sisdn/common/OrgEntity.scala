package sisdn.common

sealed trait OrgEntity {
  val id: String
  val title: String
  val titleTr: Option[String]
  val org: Option[String]
}

case class Faculty
(
  id: String,
  title: String,
  titleTr: Option[String],
  org: Option[String],
  isActive: Option[Boolean] = Some(true)
) extends OrgEntity

case class Department
(
  id: String,
  facultyId: String,
  title: String,
  titleTr: Option[String],
  org: Option[String],
  isActive: Option[Boolean] = Some(true)
) extends OrgEntity

case class Course
(
  id: String,
  departmentId: String,
  facultyId: String,
  title: String,
  titleTr: Option[String],
  remarks: Option[String],
  org: Option[String],
  isActive: Option[Boolean] = Some(true)
) extends OrgEntity

case class Program
(
  id: String,
  facultyId: String,
  terms: Int,
  creditHours: BigDecimal,
  title: String,
  titleTr: Option[String],
  org: Option[String],
  isActive: Option[Boolean] = Some(true)
) extends OrgEntity

case class OrgValidCmd(entities: List[OrgEntity])