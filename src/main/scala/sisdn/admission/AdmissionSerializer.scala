package sisdn.admission

import java.nio.charset.Charset

import akka.serialization.SerializerWithStringManifest

class AdmissionSerializer extends SerializerWithStringManifest{
  override def identifier: Int = 42

  final val submissionEvtManifest = classOf[SubmittedEvt].getName
  final val validatedEvtManifest = classOf[ValidatedEvt].getName
  final val processedEvtManifest = classOf[ProcessedEvt].getName

  val UTF_8 = Charset.forName("UTF-8")

  override def manifest(o: AnyRef): String = o match {
    case _:SubmittedEvt => submissionEvtManifest
    case _:ValidatedEvt => validatedEvtManifest
    case _:ProcessedEvt => processedEvtManifest
  }

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case SubmittedEvt(data) => {
      val student = AdmissionDataModel.Student.newBuilder().setId(data.student.get.id).
        setName(data.student.get.name).setProgram(data.student.get.program).
        setFaculty(data.student.get.faculty).setOrg(data.student.get.org)

      val admissionData = AdmissionDataModel.SubmissionData.newBuilder().setId(data.id).
        setRemarks(data.remarks).setStudent(student).setStatus(setStatus(data.status))

      AdmissionDataModel.SubmittedEvt.newBuilder().setData(admissionData).build().toByteArray
    }

    case ValidatedEvt(data) => {
      val response = AdmissionDataModel.AdmissionValidationResponse.newBuilder().
        setValid(data.valid).setRemarks(data.reason)
      AdmissionDataModel.ValidatedEvt.newBuilder().setData(response).build().toByteArray
    }

    case ProcessedEvt(data) => {
      val response = AdmissionDataModel.AdmissionProcessingResponse.newBuilder().
        setStatus(setStatus(data.status)).setRemarks(data.remarks)
      AdmissionDataModel.ProcessedEvt.newBuilder().setData(response).build().toByteArray
    }
  }


  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case `submissionEvtManifest` => AdmissionDataModel.SubmittedEvt.parseFrom(bytes)
    case `validatedEvtManifest`  => AdmissionDataModel.ValidatedEvt.parseFrom(bytes)
    case `processedEvtManifest`  => AdmissionDataModel.ProcessedEvt.parseFrom(bytes)
  }

  private def setStatus(domainStatus: AdmissionStatus.Value) = {
    import AdmissionDataModel.AdmissionStatus._; import AdmissionStatus._
    domainStatus match {
      case Accepted     => ACCEPTED
      case InProcessing => INPROCESSING
      case Invalid      => INVALID
      case Pending      => PENDING
      case Rejected     => REJECTED
      case Valid        => VALID
    }
  }
}
