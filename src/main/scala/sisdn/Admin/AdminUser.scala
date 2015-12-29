package sisdn.Admin

import akka.actor.{Props, ActorRef}
import akka.persistence.PersistentActor
import akka.util.Timeout
import scala.concurrent.duration.DurationInt
import sisdn.Admin.Organization._
import sisdn.common.{SisdnPending, SisdnReply, User}
import scala.language.postfixOps

class AdminUser(id: String, org: ActorRef) extends PersistentActor {
  import AdminUser._
  override def persistenceId: String = id

  implicit val ec = context.dispatcher
  implicit val timeout: Timeout = 3 second

  override def receiveRecover: Receive = ???

  override def receiveCommand: Receive = {
    case e: OrgCmd => {
      if(userHasClaim(e.user, e.entity) ){
        persist(e){ evt => context.system.log.info(s"$evt")}
      }
    }
  }


}

object AdminUser{
  def props(id: String, org: ActorRef) = Props(new AdminUser(id, org))

  def userHasClaim(user: User, entity: OrganizationEntity): Boolean = entity match {
    case e:Faculty => user.claims.exists(_.contains("admin_" + e.org))

    case e: Department => user.claims.exists(c => c.contains("admin_" + e.org) ||
      c.contains("admin_" + e.facultyId))

    case e:Program => user.claims.exists(c => c.contains("admin_" + e.org) ||
      c.contains("admin_" + e.facultyId))

    case e:Course => user.claims.exists(c => c.contains("admin_" + e.org) ||
      c.contains("admin_" + e.facultyId) || c.contains("admin_" + e.departmentId))
  }



  class State{
    var commands = Map[String, Map[OrgCmd, SisdnReply]]()
    def updateState(evt: Any): Unit = evt match {
      case e: OrgCmd => commands + (e.id -> Map(e -> SisdnPending(e.id)))
    }
  }
}