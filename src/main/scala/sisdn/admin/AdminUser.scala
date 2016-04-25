package sisdn.admin

import akka.actor.{ActorSystem, ActorLogging, Props, ActorRef}
import akka.persistence.PersistentActor
import akka.util.Timeout
import scala.concurrent.duration.DurationInt
import sisdn.admin.Organization._
import sisdn.common.{SisdnPending, SisdnReply}
import scala.language.postfixOps

class AdminUser(userId: String, org: ActorRef) extends PersistentActor with ActorLogging {
  import AdminUser._
  override def persistenceId: String = userId

  implicit val ec = context.dispatcher
  implicit val timeout: Timeout = 3 second

  var state = new State(context.system)

  override def receiveRecover: Receive = {
    case evt: OrganizationEvt => state.update(evt)
  }

  override def receiveCommand: Receive = {
    case cmd: OrgCmd => persist(cmd){ evt =>
        org forward cmd
        context.system.log.info(s"$evt")
      }
  }
}

object AdminUser{
  def props(userId: String, org: ActorRef) = Props(new AdminUser(userId, org))

  class State(system: ActorSystem) {
    var commands = Map[String, Map[OrgCmd, SisdnReply]]()
    def update(evt: Any): Unit = evt match {
      case e: OrgCmd => commands + (e.id -> Map(e -> SisdnPending(e.id)))
    }
  }
}