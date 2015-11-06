package sisdn.admission.service

import akka.actor._
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import sisdn.admission.model._
import sisdn.admission.service.AdmissionFSM._
import scala.concurrent.duration._
import scala.concurrent.{Future, ExecutionContext}
import scala.reflect.{ClassTag, classTag}
import scala.language.postfixOps
import sisdn.admission.utils.Conversions._

class AdmissionFSM(id: String, validatorActor: ActorRef, processorActor: ActorRef)
  extends PersistentFSM[State, AdmissionData, AdmissionEvt] with ActorLogging {

  override def persistenceId: String = id

  override def domainEventClassTag: ClassTag[AdmissionEvt] = classTag[AdmissionEvt]

  implicit val ec = context.dispatcher
  implicit val Timeout = 3 seconds
  val validator = validatorActor
  val processor = processorActor
  val config = ConfigFactory.load().getConfig("sisdn.admission")
  //flag to facilitate testing
  val `Testing` = ConfigFactory.load().getBoolean("sisdn.testing")

  //TODO see if you can fix non exhaustive match
  override def applyEvent(evt: AdmissionEvt, currentData: AdmissionData): AdmissionData = evt match {
    case SubmittedEvt(data) => data.copy(status = AdmissionStatus.Pending, remarks = "")

    case ValidatedEvt(ValidAdmission) => currentData.asInstanceOf[SubmissionData].
      copy(status = AdmissionStatus.Valid)

     case ValidatedEvt(InvalidAdmission(reason)) => currentData.asInstanceOf[SubmissionData].
       copy(remarks = reason, status = AdmissionStatus.Invalid)

    case ProcessedEvt(Accepted) => currentData.asInstanceOf[SubmissionData].
      copy(status = AdmissionStatus.Accepted)

    case ProcessedEvt(Rejected(reason)) => currentData.asInstanceOf[SubmissionData].
      copy(status = AdmissionStatus.Rejected, remarks = reason)

  }

  startWith(InitState, EmptyAdmissionData)

  when(InitState) {
    case Event(SubmittedEvt(data), _) => goto(PendingValidationState).
      applying(SubmittedEvt(data)) replying ACK andThen {
      case _ => validator ! data.student
    }
  }

  when(PendingValidationState, stateTimeout =  config.getDuration("validationResponseTimeout")) {
    case Event(ValidatedEvt(data @ ValidAdmission), stateData) => goto(ValidState).
      applying (ValidatedEvt(data)) andThen { case _ => processor ! stateData.student }
    case Event(ValidatedEvt(InvalidAdmission(reason)), _) =>
      goto(InvalidState) applying ValidatedEvt(InvalidAdmission(reason))
    case Event(StateTimeout, data) => goto(PendingValidationState) andThen {case_ => validator ! data.student }
  }


  when(ValidState, config.getDuration("processingAckTimeout")){
    case Event(ACK, _) => goto(InProcessingState)
    case Event(StateTimeout, data) => goto(ValidState) andThen {case_ => processor ! data.student }
  }

  //TODO need to decide what to do if admission was invalid
  when(InvalidState, config.getDuration("invalidStateDUration")){
    case Event(ACK, _) => stop()
  }

  when(InProcessingState, config.getDuration("processingResponseTimeout")){
    case Event(evt @ ProcessedEvt(Accepted), _) => goto(AcceptedState) applying evt
    case Event(evt @ ProcessedEvt(Rejected(reason)), _) => goto(RejectedState) applying evt
    case Event(StateTimeout, data) => goto(InProcessingState) andThen{case _ => processor ! data.student}
  }

  //TODO had to put "ACK" so they don't intercept all events
  when(RejectedState){ case Event(ACK,_) => stay()}
  when(AcceptedState){ case Event(ACK,_) => stay()}

  whenUnhandled{
      case Event(e,s) if `Testing` && e == "state" => stay replying s
  }

  initialize()
}

object AdmissionFSM {
  def props(id: String, validator: ActorRef, processor: ActorRef) =
    Props(classOf[AdmissionFSM], id, validator, processor)




  sealed trait State extends FSMState

  case object InitState extends State {
    override def identifier: String = "InitState"
  }

  case object PendingValidationState extends State {
    override def identifier: String = "PendingValidationState"
  }

  case object InvalidState extends State {
    override def identifier: String = "InvalidState"
  }

  case object ValidState extends State {
    override def identifier: String = "PendingProcessingState"
  }

  case object InProcessingState extends State {
    override def identifier: String = "InProcessingState"
  }

  case object AcceptedState extends State {
    override def identifier: String = "AcceptedState"
  }

  case object AdmittedState extends State {
    override def identifier: String = "AdmittedState"
  }

  case object RejectedState extends State {
    override def identifier: String = "RejectedState"
  }
}
