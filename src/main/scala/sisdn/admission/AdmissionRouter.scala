package sisdn.admission

import akka.actor.{Actor, ActorLogging, Props}
import sisdn.admission.AdmissionUser.Admit
import sisdn.common.SisdnInvalid

class AdmissionRouter extends Actor with ActorLogging {
  override def receive: Receive = {
    case e: Admit =>
      val validator = context.actorOf(Props(classOf[ValidatorActor]), "validator" + e.user.org)
      val processor = context.actorOf(Props(classOf[ProcessorActor]), "processor" + e.user.org)
      val admitter = context.actorOf(AdmissionFSM.props(e.id, e.user, validator, processor))
      val admissionUser = context.actorOf(AdmissionUser.props(e.user, admitter))
      admissionUser ! e
    case _ => log.debug("received unknown command")
      sender() ! SisdnInvalid
  }
}
