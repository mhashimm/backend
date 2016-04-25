package sisdn.admission

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import sisdn.common.{SisdnCreated, SisdnDuplicate, SisdnInvalid, User, uuid}

class AdmissionUser(userId: String, admitter: ActorRef)
  extends PersistentActor with ActorLogging {

  import AdmissionStatus._
  import AdmissionUser._

  override def persistenceId = userId

  val state = new UserState

  def updateState(evt: AdmissionStatusUpdateEvt) = {
    if (evt.status == Pending) {
      log debug s"Admission ${evt.id} Pending"
      state.update(evt)
      admitter ! evt
    }
    else if (evt.status == Valid) {
      log debug s"Admission ${evt.id} Valid"
      state.update(evt)
      admitter ! evt
    }
    else if (evt.status == Rejected || evt.status == Accepted) {
      log.debug(s"Admission ${evt.id} ${evt.status}")
      state.update(evt)
    }
  }

  def receiveRecover = {
    case admissionEvt: AdmissionStatusUpdateEvt => updateState(admissionEvt)
  }

  def receiveCommand = {
    case admission: Admission => {
      if(!state.admissions.contains(admission.id)) {
        persist(AdmissionStatusUpdateEvt(admission.id, AdmissionStatus.Pending, "")) { evt =>
          sender() ! SisdnCreated(admission.id)
          admitter ! SubmittedEvt(
            NonEmptyAdmissionData(
              uuid,
              admission.id,
              Some(admission.student),
              AdmissionStatus.Pending,
              "",
              userId ))
        }
      }
      else
        sender() ! SisdnDuplicate(admission.id, "")
    }

    case _ => sender() ! SisdnInvalid("", "Unknown command")
  }
}

object AdmissionUser {
  def props(userId: String, admitter: ActorRef) = Props(new AdmissionUser(userId, admitter))

  case class Admission(id: String, user: User, student: Student)

  case class AdmissionStatusUpdateEvt(id: String, status: AdmissionStatus.Value, remarks: String)

  class UserState {
    var admissions = Map[String, AdmissionStatusUpdateEvt]()

    def update(data: AdmissionStatusUpdateEvt) = {
      admissions = admissions + admissions.get(data.id).map { a =>
        (a.id, AdmissionStatusUpdateEvt(a.id, data.status, data.remarks))
      }.get
    }
  }

}