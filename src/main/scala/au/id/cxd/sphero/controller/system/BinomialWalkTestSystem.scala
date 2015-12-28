package au.id.cxd.sphero.controller.system

import akka.actor.{Props, ActorSystem, ActorSelection}
import au.id.cxd.sphero.controller.agents.{BinomialCollisionController, RandomController, Coordinator}
import au.id.cxd.sphero.controller.bluetooth.ConnectThread

/**
  *
  * This system will allow the user to test a walk through the space
  * using a binomial model for selecting the least likely direction to head in
  * given a collision
  *
  * Created by cd on 24/12/2015.
  */
class BinomialWalkTestSystem extends Coordinator  {

  /**
    * abstract method to produce an actor ref
    * @return
    */
  def robotController():ActorSelection = context.system.actorSelection(BinomialCollisionController.actorPath)

  /**
    * access the connector thread
    * @return
    */
  def connectorThread():ActorSelection = context.system.actorSelection(ConnectThread.actorPath)

}

object BinomialWalkTestSystem {
  def apply(system: ActorSystem) = {
    val sys = system.actorOf(Props[BinomialWalkTestSystem], "binomWalk")
    val robot = BinomialCollisionController(system, sys)
    val connector = ConnectThread(system, sys)
    sys
  }
}