package sisdn.admission

import akka.actor.{Actor, ActorLogging, Props}
import sisdn.admission.AdmissionUser.Admission
import sisdn.common.{ SisdnUnauthorized, SisdnInvalid}

class AdmissionRouter extends Actor with ActorLogging {
  import AdmissionRouter._
  override def receive: Receive = {
    case admission: Admission =>
        val validator = context.actorOf(Props(classOf[ValidatorActor]), "validator" + admission.user.org)
        val processor = context.actorOf(Props(classOf[ProcessorActor]), "processor" + admission.user.org)
        val admitter = context.actorOf(AdmissionFSM.props(admission.id, admission.user.username, validator, processor))
        val admissionUser = context.actorOf(AdmissionUser.props(admission.user.username, admitter))
    case _ => sender() ! SisdnInvalid
  }
}

object AdmissionRouter {
  def props() = Props(classOf[AdmissionRouter])
}
