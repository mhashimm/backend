package sisdn

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import sisdn.common.{Program, Course, Department, Faculty}
import spray.json.DefaultJsonProtocol

package object admin {
  trait OrgJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val facultyFormat = jsonFormat5(Faculty.apply)
    implicit val departmentFormat = jsonFormat6(Department.apply)
    implicit val courseFormat = jsonFormat8(Course.apply)
    implicit val programFormat = jsonFormat8(Program.apply)
  }

}
