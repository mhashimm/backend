package registration.test

import java.io.InputStream

import spray.json._

import scala.io.Source
import org.scalatest.{Matchers, FlatSpec}
import registration.EnrolmentModule

class EnrolmentModuleSpecs extends FlatSpec with Matchers{
  import registration.RegistrationJsonProtocol._

  val stream : InputStream = getClass.getResourceAsStream( "/enrol.json")
  val moduleJson = Source.fromInputStream( stream ).mkString
  val model = JsonParser(moduleJson).convertTo[EnrolmentModule]

  "EnrolmentModule" should "be parsed correctly for correct json string" in {
    model.org shouldEqual "1"
  }

  it should """fail for wrong "programId" element""" in {
    val wrongModel = moduleJson.replaceAll(""""programId": "program1",""", """"programI": "program1",""")
    intercept[DeserializationException]{
      JsonParser(wrongModel).convertTo[EnrolmentModule]
    }
  }

  it should """fail for absent courses element""" in {
    val wrongModel = model.copy(courses = Set()).toJson.toString().replaceAll(""""courses":\[\],""", "")
    intercept[DeserializationException]{
      JsonParser(wrongModel).convertTo[EnrolmentModule]
    }
  }
}
