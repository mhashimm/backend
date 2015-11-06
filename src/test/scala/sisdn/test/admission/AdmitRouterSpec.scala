/*
package sisdn.admission.test

import akka.actor.{Props, ActorSystem}
import akka.stream.ActorMaterializer
import akka.testkit._
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpecLike}
import sisdn.admission.model.{Student, User}
import sisdn.admission.service.UserActor
import sisdn.admission.service.UserActor.Admit
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class AdmitRouterSpec (_system: ActorSystem) extends TestKit(_system) with ImplicitSender
                with FlatSpecLike with Matchers with BeforeAndAfterAll  {
  implicit val ec = system.dispatcher
  implicit val mat =  ActorMaterializer()
  implicit val timeout: Timeout = 1 second
  def this() = this(ActorSystem("AdmitRouterSpec"))
  override def afterAll { TestKit.shutdownActorSystem(system) }

  val userActor = system.actorOf(Props (UserActor("1", testActor)

  "An AdmitRouter actor" should "route the message unchanged" in {
      val actor = system.actorOf(UserActor.props("1"))
      actor ! Admit(User("1","",None,None), List[Student]())
      expectMsg()
    }
}
*/
