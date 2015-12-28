package au.id.cxd.sphero.controller

import akka.actor.{Props, ActorSystem}
import au.id.cxd.sphero.controller.protocol.Search
import au.id.cxd.sphero.controller.system.SystemBuilder
import akka.actor._

/**
  * Created by cd on 24/12/2015.
  */
trait SystemRunner {
   this: SystemBuilder =>


  def run(system:ActorSystem) = {
    val parent = build(system)
    parent ! Search()
    system.actorOf(Props(classOf[Terminator], parent), "terminator")
  }

}
