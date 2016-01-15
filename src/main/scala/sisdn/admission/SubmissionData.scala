package sisdn.admission

case class AdmissionValidationResponse(valid: Boolean, reason: String)
case class AdmissionProcessingResponse(status: AdmissionStatus.Value, remarks: String)

sealed trait AdmissionEvt
case class SubmittedEvt(data: AdmissionData) extends AdmissionEvt
case class ValidatedEvt(data: AdmissionValidationResponse) extends AdmissionEvt
case class ProcessedEvt(data: AdmissionProcessingResponse) extends AdmissionEvt

// General Acknowledgement object
case object ACK

object AdmissionStatus extends Enumeration {
  val Valid, Invalid, Pending, InProcessing, Accepted, Rejected = Value
}

sealed trait AdmissionData {
  val id: String = ""
  val student: Option[Student] = None
  val status: AdmissionStatus.Value = AdmissionStatus.Pending
  val remarks: String = ""
}

case class NonEmptyAdmissionData
(
  override val id: String,
  override val student: Option[Student],
  override val status: AdmissionStatus.Value,
  override val remarks: String
) extends AdmissionData

case object EmptyAdmissionData extends AdmissionData