package au.id.cxd.sphero.controller.system

import java.lang.Shutdown

import akka.actor._
import au.id.cxd.sphero.controller._
import au.id.cxd.sphero.controller.agents.{RandomController, Coordinator, RobotController}
import au.id.cxd.sphero.controller.bluetooth.ConnectThread
import au.id.cxd.sphero.controller.protocol.{Stop, Connected, StopSearch, Search}
import se.nicklasgavelin.sphero.Robot
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit;

/**
  * Created by cd on 12/09/2015.
  */
class RandomWalkTestSystem() extends Coordinator {


  import context.dispatcher
  /**
    * abstract method to produce an actor ref
    * @return
    */
  def robotController():ActorSelection = context.system.actorSelection(RandomController.actorPath)

  /**
    * access the connector thread
    * @return
    */
  def connectorThread():ActorSelection = context.system.actorSelection(ConnectThread.actorPath)

}

object RandomWalkTestSystem {

  def apply(system: ActorSystem):ActorRef = {
    val test = system.actorOf(Props[RandomWalkTestSystem], "test")
    val robot = RandomController(system, test)
    val connector = ConnectThread(system, test)
    test
  }

}
