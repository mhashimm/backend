package sisdn.admission.model

sealed trait ValidationData
case object ValidAdmission extends ValidationData
case class InvalidAdmission(reason: String) extends ValidationData

sealed trait AdmissionEvt
case class SubmittedEvt(data: SubmissionData) extends AdmissionEvt
case class ValidatedEvt(data: ValidationData) extends AdmissionEvt
case class ProcessedEvt(data: ProcessResponseData) extends AdmissionEvt

sealed trait ProcessResponseData
case object Accepted extends ProcessResponseData
case class Rejected(reason: String) extends ProcessResponseData

// General Acknowledgement object
case object ACK

object AdmissionStatus extends Enumeration {
  val Valid, Invalid, Pending, Accepted, Rejected = Value
}

trait AdmissionData {
  val id: String
  val student: Student
  val status: AdmissionStatus.Value
  val remarks: String
}

case class SubmissionData
(
  id: String,
  student: Student,
  status: AdmissionStatus.Value,
  remarks: String
) extends AdmissionData

case object EmptyAdmissionData extends AdmissionData {
  val id: String = ""
  val student = null
  val status = AdmissionStatus.Pending
  val remarks = ""
}