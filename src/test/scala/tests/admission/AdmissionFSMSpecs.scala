package tests.admission

import akka.actor.{ActorRef, ActorSystem}
import akka.persistence.fsm.PersistentFSM._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest._
import scala.language.postfixOps
import scala.concurrent.duration._

import sisdn.common.{User, asFiniteDuration}
import sisdn.admission._
import sisdn.common.uuid

class AdmissionFSMSpecs(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with FlatSpecLike with Matchers with BeforeAndAfterAll {

  import AdmissionFSM._

  def this() = this(ActorSystem("AdmissionSpec"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  val config = ConfigFactory.load().getConfig("sisdn.admission")
  val validTimeout = config.getDuration("validationResponseTimeout")
  val processTimeout = config.getDuration("processingAckTimeout")

  val user = User("user1", "org", None, None, None)

  def admissionFunc(id: String, user: User, valid: ActorRef, proc: ActorRef): ActorRef =
    system.actorOf(AdmissionFSM.props(id, user.username, valid, proc))

  def admissionData(id: String) = NonEmptyAdmissionData(uuid, id, Some(Student(id, "", "1", "1", "org")),
    AdmissionStatus.Pending, "", user.username)


  "Admission actor" should "acknowledge received and save admission" in {
    val valid, proc, driver = TestProbe()
    val id = "1"
    val admission = admissionFunc(id, user, valid.ref, proc.ref)
    val aData = admissionData(id)
    admission ! SubmittedEvt(aData)
    expectMsg(ACK)

    admission.tell("state", driver.ref)
    driver.expectMsgPF(){
      case current: AdmissionData => current.student shouldEqual aData.student
    }
  }

  it should "invoke Validation service" in {
    val valid, proc, driver = TestProbe()
    val id = "2"
    val admission = admissionFunc(id, user, valid.ref, proc.ref)
    admission.tell(SubscribeTransitionCallBack(driver.ref), driver.ref)
    val aData = admissionData(id)
    admission.tell(SubmittedEvt(aData), driver.ref)
    valid.expectMsg(aData.student)
  }

  it should """respond to positive validation by moving to "ValidState" """ in {
    val driver, valid, proc = TestProbe()
    val id = "3"
    val aData = admissionData(id)
    val admission = admissionFunc(id, user, valid.ref, proc.ref)
    admission.tell(SubscribeTransitionCallBack(driver.ref), driver.ref)
    admission.tell(SubmittedEvt(aData), driver.ref)
    valid.expectMsg(aData.student)
    valid.reply(ValidatedEvt( AdmissionValidationResponse(true, "")))
    driver.receiveN(3)
    driver.expectMsgPF(validTimeout, "") {
      case Transition(_, PendingValidationState, ValidState, _) => true
    }

    admission.tell("state", driver.ref)
    driver.expectMsgPF(validTimeout, "") {
      case current: AdmissionData => current.status shouldBe AdmissionStatus.Valid
    }

  }

  it should """respond to negative validation by moving to "InvalidState" """ in {
    val driver, valid, proc = TestProbe()
    val id = "4"
    val aData = admissionData(id)
    val admission = admissionFunc(id, user, valid.ref, proc.ref)
    admission.tell(SubscribeTransitionCallBack(driver.ref), driver.ref)
    admission.tell(SubmittedEvt(aData), driver.ref)
    valid.expectMsg(aData.student)
    valid.reply(ValidatedEvt(AdmissionValidationResponse(false, "")))

    driver.receiveN(3)
    driver.expectMsgPF() {
      case Transition(_, PendingValidationState, InvalidState, _) => true
    }

    admission.tell("state", driver.ref)
    driver.expectMsgPF() {
      case current: AdmissionData => current.status shouldEqual AdmissionStatus.Invalid
    }
  }

  it should """stay in "PendingValidation" and retry when no response is received from validation service""" in {
    val driver, valid, proc = TestProbe()
    val id = "5"
    val admission = admissionFunc(id, user, valid.ref, proc.ref)
    val aData = admissionData(id)
    admission.tell(SubmittedEvt(aData), driver.ref)
    valid.receiveN(2, validTimeout * 2)
  }

  it should """move to "InProcessingState" after receiving confirmation from processor""" in {
    val driver, valid, proc = TestProbe()
    val id = "6"
    val admission = admissionFunc(id, user, valid.ref, proc.ref)
    val aData = admissionData(id)

    admission.tell(SubmittedEvt(aData), driver.ref)

    valid.expectMsg(aData.student)
    valid.reply(ValidatedEvt(AdmissionValidationResponse(true, "")))
    proc.expectMsg(aData.student)
    admission.tell(SubscribeTransitionCallBack(driver.ref), driver.ref)
    proc.reply(ProcessedEvt(AdmissionProcessingResponse(AdmissionStatus.InProcessing, "")))
    driver.receiveN(2)
    driver.expectMsgPF() {
      case Transition(_, ValidState, InProcessingState, _) => true
    }

    admission.tell("state", driver.ref)
    driver.expectMsgPF() {
      case current: AdmissionData => current.status shouldEqual AdmissionStatus.InProcessing
    }

  }

  it should """stay in "ValidState" and keep retrying processor if not confirmed""" in {
    val driver, valid, proc = TestProbe()
    val id = "7"
    val admission = admissionFunc(id, user, valid.ref, proc.ref)
    val aData = admissionData(id)

    admission.tell(SubmittedEvt(aData), driver.ref)

    valid.expectMsg(aData.student)
    valid.reply(ValidatedEvt(AdmissionValidationResponse(true, "")))
    proc.receiveN(2, validTimeout * 3)
  }

  it should """move to "AcceptedState" after being accepted""" in {
    val driver, valid, proc = TestProbe()
    val id = "8"
    val admission = admissionFunc(id, user, valid.ref, proc.ref)
    val aData = admissionData(id)


    admission.tell(SubmittedEvt(aData), driver.ref)

    valid.expectMsg(aData.student)
    valid.reply(ValidatedEvt(AdmissionValidationResponse(true, "")))
    proc.expectMsg(aData.student)
    proc.reply(ProcessedEvt(AdmissionProcessingResponse(AdmissionStatus.InProcessing, "")))
    admission.tell(SubscribeTransitionCallBack(driver.ref), driver.ref)
    proc.send(admission, ProcessedEvt(AdmissionProcessingResponse(AdmissionStatus.Accepted, "")))
    driver.receiveN(2)
    driver.expectMsgPF(processTimeout, "") {
      case Transition(_, InProcessingState, AcceptedState, _) => true
    }

    admission.tell("state", driver.ref)
    driver.expectMsgPF() {
      case current: AdmissionData => current.status shouldEqual AdmissionStatus.Accepted
    }
  }

  it should """move to "RejectedState" after being rejected""" in {
    val driver, valid, proc = TestProbe()
    val id = "9"
    val admission = admissionFunc(id, user, valid.ref, proc.ref)
    val aData = admissionData(id)


    admission.tell(SubmittedEvt(aData), driver.ref)

    valid.expectMsg(aData.student)
    valid.reply(ValidatedEvt(AdmissionValidationResponse(true, "")))
    proc.expectMsg(aData.student)
    proc.reply(ProcessedEvt(AdmissionProcessingResponse(AdmissionStatus.InProcessing, "")))
    admission.tell(SubscribeTransitionCallBack(driver.ref), driver.ref)
    proc.send(admission, ProcessedEvt(AdmissionProcessingResponse(AdmissionStatus.Rejected, "rejected")))

    driver.receiveN(2)
    driver.expectMsgPF(validTimeout, "") {
      case Transition(_, InProcessingState, RejectedState, _) => true
    }

    admission.tell("state", driver.ref)
    driver.expectMsgPF(){
      case current: AdmissionData => current.status shouldEqual AdmissionStatus.Rejected
    }
  }
}
