package sisdn.admission

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor

class AdmissionUser(id: String, admiter: ActorRef)
  extends PersistentActor with ActorLogging {

  import AdmissionStatus._
  import AdmissionUser._

  override def persistenceId = id

  val state = new UserState

  def updateState(evt: AdmissionStatusUpdateEvt) = {
    if (evt.status == Pending) {
      log debug s"Admission ${evt.id} Pending"
      state.update(evt)
      admiter ! evt
    }
    else if (evt.status == Valid) {
      log debug s"Admission ${evt.id} Valid"
      state.update(evt)
      admiter ! evt
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
    case Admit(_, students) => {
      persistAll(students.filter(std => state.admissions.exists(_._1 != std.id))) _ //TODO handle callback
    }
  }
}

object AdmissionUser {
  def props(id: String, admiter: ActorRef) = Props(new AdmissionUser(id, admiter))

  case class Admit(user: User, students: List[Student])

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