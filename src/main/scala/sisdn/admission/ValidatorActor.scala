package sisdn.admission

import akka.actor.{Actor, ActorLogging}


class ValidatorActor  extends Actor with ActorLogging {
  override def receive: Receive = {
    case e: Student => sender ! ValidatedEvt(AdmissionValidationResponse(true, ""))
  }
}
