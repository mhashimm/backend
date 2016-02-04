package sisdn.admin

import akka.actor.{ActorSystem, ActorLogging, Props, ActorRef}
import akka.persistence.PersistentActor
import akka.util.Timeout
import scala.concurrent.duration.DurationInt
import sisdn.admin.Organization._
import sisdn.common.{SisdnUnauthorized, SisdnPending, SisdnReply, User}
import scala.language.postfixOps

class AdminUser(id: String, org: ActorRef) extends PersistentActor with ActorLogging {
  import AdminUser._
  override def persistenceId: String = id

  implicit val ec = context.dispatcher
  implicit val timeout: Timeout = 3 second

  var state = new State(context.system)

  override def receiveRecover: Receive = {
    case evt: OrganizationEvt => state.update(evt)
  }

  override def receiveCommand: Receive = {
    case cmd: OrgCmd if userHasClaim(cmd.user, cmd.entity)  =>
      persist(cmd){ evt =>
        org forward cmd
        context.system.log.info(s"$evt")
      }
    case cmd: OrgCmd if !userHasClaim(cmd.user, cmd.entity) => persist(cmd) { evt =>
      sender() ! SisdnUnauthorized(evt.id)
      context.system.log.info(s"An unauthorized attempt to access admin portal with  this event $evt")
    }
  }
}

object AdminUser{
  def props(id: String, org: ActorRef) = Props(new AdminUser(id, org))

  def userHasClaim(user: User, entity: OrganizationEntity): Boolean = entity match {
    case e:Faculty => user.claims.exists(_.contains("admin_" + e.org.get))

    case e: Department => user.claims.exists(c => c.contains("admin_" + e.org.get) ||
      (c.contains("admin_" + e.facultyId) && e.org.get == user.org ))

    case e:Program => user.claims.exists(c => c.contains("admin_" + e.org.get) ||
      (c.contains("admin_" + e.facultyId) && e.org.get == user.org ))

    case e:Course => user.claims.exists(c => c.contains("admin_" + e.org.get) ||
      (c.contains("admin_" + e.facultyId) || c.contains("admin_" + e.departmentId)
        && e.org.get == user.org))
  }



  class State(system: ActorSystem) {
    var commands = Map[String, Map[OrgCmd, SisdnReply]]()
    def update(evt: Any): Unit = evt match {
      case e: OrgCmd => commands + (e.id -> Map(e -> SisdnPending(e.id)))
    }
  }
}