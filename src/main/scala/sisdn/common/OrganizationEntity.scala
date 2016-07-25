package sisdn.common

sealed trait OrganizationEntity {
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
) extends OrganizationEntity

case class Department
(
  id: String,
  facultyId: String,
  title: String,
  titleTr: Option[String],
  org: Option[String],
  isActive: Option[Boolean] = Some(true)
) extends OrganizationEntity

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
) extends OrganizationEntity

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
) extends OrganizationEntity
