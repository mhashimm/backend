package sisdn.registration

//TODO This should be converted to xml and have xsd schema
//TODO Does spray conversion is valid BigDecimal ????
//TODO Do we need "creditable" in addition to gradable
case class EnrolmentModule
(
  org: String,
  programId: String,
  level: Int,
  remarks: String,
  tags: String,
  gradingScale: GradingScale,
  courses: Set[CourseModule]
)

case class CourseModule
(
  id: String,
  groupingMethod: GroupingMethod.GroupingMethodType,
  hours: BigDecimal,
  major: Boolean,
  compulsory: Boolean,
  totalDegree: BigDecimal,
  gradables: Set[GradableType],
  gradingScala: Option[GradingScale]
)

case class GradingScale(passMark: BigDecimal, grades: List[Grade])

case class GradableType(name: String, degree: BigDecimal)

case class Grade
(
  value: String,
  min: BigDecimal,
  max: BigDecimal,
  weight: BigDecimal
)

object GroupingMethod extends Enumeration {
  type GroupingMethodType = Value
  val standard = Value("standard")
  val atomic = Value("atomic")
  val optimistic = Value("optimistic")
}

