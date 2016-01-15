package sisdn

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

package object admission {
  trait StudentJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val studentFormat = jsonFormat5(Student.apply)
  }
}
