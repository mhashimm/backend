package registration

import spray.json._

object RegistrationJsonProtocol extends DefaultJsonProtocol {
  def jsonEnum[T <: Enumeration](enu: T) = new JsonFormat[T#Value] {
    def write(obj: T#Value) = JsString(obj.toString)

    def read(json: JsValue) = json match {
      case JsString(txt) => enu.withName(txt)
      case something => throw new DeserializationException(s"Expected a value from enum $enu instead of $something")
    }
  }
  implicit val groupingMethodFormat = jsonEnum(GroupingMethod)
  implicit val gradeFormat = jsonFormat4(Grade)
  implicit val gradableTypeFormat = jsonFormat2(GradableType)
  implicit val gradingScaleFormat = jsonFormat2(GradingScale)
  implicit val courseModuleFormat = jsonFormat8(CourseModule)
  implicit val enrolmentFormat = jsonFormat7(EnrolmentModule)
}
