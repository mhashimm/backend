package sisdn.admission

import akka.actor.{Actor, ActorLogging}

class ProcessorActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case student: Student => sender() ! ProcessedEvt(AdmissionProcessingResponse(AdmissionStatus.InProcessing, ""))
  }
}
