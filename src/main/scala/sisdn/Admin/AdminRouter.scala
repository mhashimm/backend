package sisdn.Admin

import akka.actor.{ActorLogging, ActorSystem, Actor}
import Organization._
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

class AdminRouter extends Actor with ActorLogging {

  override def receive: Receive = {
    case cmd:OrgCmd =>
      val actor = context.actorOf(Organization.props(cmd.user.org))
      actor forward cmd
    case _ => log.debug("received hello"); sender() ! "hello"
  }
}
