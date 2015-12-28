package au.id.cxd.sphero.controller

import akka.actor.{Terminated, ActorLogging, Actor, ActorRef}

/**
  * a deatch watch agent
  * Created by cd on 24/12/2015.
  */
class Terminator(ref: ActorRef) extends Actor with ActorLogging {
  context watch ref
  def receive = {
    case Terminated(_) =>
      log.info("{} has terminated, shutting down system", ref.path)
      context.system.shutdown()
  }
}
