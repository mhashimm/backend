package sisdn.admission

import akka.actor.{Actor, ActorLogging, Props}
import sisdn.admin.Organization
import sisdn.common.{Faculty, OrgValidCmd, Program}


class ValidatorActor(orgId: String) extends Actor with ActorLogging {
  override def receive: Receive = {
    case student: Student =>
      context.actorOf(Props(new Actor() {
        val org = context.actorOf(Organization.props(orgId))
        val originalSender = sender
        def receive = {
          case true => originalSender ! ValidatedEvt(AdmissionValidationResponse(true, ""))
          case _    => originalSender ! ValidatedEvt(AdmissionValidationResponse(false, ""))
        }
        org ! OrgValidCmd(List(Program(student.program, "", 0, 0, "", None, None, None),
          Faculty(student.faculty, "", None, None, None)))
      }))
  }
}
