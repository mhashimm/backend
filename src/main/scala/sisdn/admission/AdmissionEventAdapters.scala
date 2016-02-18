package sisdn.admission

import akka.actor.ExtendedActorSystem
import akka.persistence.journal.{WriteEventAdapter, EventSeq, EventAdapter}

class AdmissionEventAdapters(system: ExtendedActorSystem) extends WriteEventAdapter {
  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any =
    event // identity

}
