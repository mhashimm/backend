package sisdn.admin

import akka.actor.{ActorLogging, Actor}
import Organization._
import sisdn.common.SisdnInvalid

class AdminRouter extends Actor with ActorLogging {

  override def receive: Receive = {
    case cmd:OrgCmd =>
      val org = context.system.actorOf(Organization.props(cmd.user.org))
      val adminUser = context.actorOf(AdminUser.props(cmd.user.username, org))
      adminUser forward cmd
    case _ => log.debug("received unknown command")
      sender() ! SisdnInvalid
  }
}
