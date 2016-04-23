package sisdn.admission

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import sisdn.common.User

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
    case admissionRvt: AdmissionStatusUpdateEvt => updateState(admissionRvt)
  }

  def receiveCommand = {
    case Admit(_, _, student) => {
      persist(state.admissions.exists(_._1 != student.id)) _ //TODO handle callback
    }
  }
}

object AdmissionUser {
  def props(user: User, admiter: ActorRef) = Props(new AdmissionUser(user, admiter))

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