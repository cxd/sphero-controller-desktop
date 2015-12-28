package au.id.cxd.sphero.controller.system

import akka.actor.{ActorRef, ActorSystem}
import au.id.cxd.sphero.controller.agents.Coordinator

/**
  * Created by cd on 24/12/2015.
  */
trait SystemBuilder {

  /**
    * build a system and produce a coordinator
    * @return
    */
  def build(system:ActorSystem):ActorRef

}
