package sisdn.admission

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import sisdn.common.{SisdnCreated, SisdnDuplicate, User, uuid}

class AdmissionUser(user: User, admitter: ActorRef)
  extends PersistentActor with ActorLogging {

  import AdmissionStatus._
  import AdmissionUser._

  override def persistenceId = user.username

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
    case admit: Admit => {
      if(!state.admissions.contains(admit.id)) {
        persist(AdmissionStatusUpdateEvt(admit.id, AdmissionStatus.Pending, "")) { evt =>
          sender() ! SisdnCreated(admit.id)
          admitter ! SubmittedEvt(
            NonEmptyAdmissionData(
              uuid,
              admit.id,
              Some(admit.student),
              AdmissionStatus.Pending,
              "",
              Some(admit.user) ))
        }
      }
      else
        sender() ! SisdnDuplicate(admit.id, "")
    }
  }
}

object AdmissionUser {
  def props(user: User, admitter: ActorRef) = Props(new AdmissionUser(user, admitter))

  case class Admit(id: String, user: User, student: Student)

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