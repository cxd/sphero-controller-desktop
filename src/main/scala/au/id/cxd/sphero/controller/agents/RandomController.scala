package au.id.cxd.sphero.controller.agents

import akka.actor.{Props, ActorSystem, ActorRef}
import au.id.cxd.sphero.controller.protocol.Update
import au.id.cxd.sphero.controller.state.model.State
import se.nicklasgavelin.sphero.Robot
import se.nicklasgavelin.sphero.command.RollCommand

import scala.util.Random

/**
  * Created by cd on 24/12/2015.
  */
class RandomController(parent: ActorRef) extends RobotController(parent) {

  override def receive = {

    case Update(robot) => {
      changeDirection(robot, state, false)
    }

    case any => super.receive (any)

  }


  /**
    * change direction randomly
    * @param robot
    * @param state
    */
  def changeDirection(robot: Robot, state: State, wasCollision: Boolean): Unit = {
    val angle = randomHeading(robot, state, lastState, wasCollision)
    if (wasCollision) {
      // rotate first
      val command1 = new RollCommand(angle.toFloat, constSpeed, true)
      robot.sendCommand(command1)
    }
    val command = new RollCommand(angle.toFloat, constSpeed, false)
    robot.sendCommand(command)
    state.setHeading = angle.toInt
    state.lastSetHeading = lastState.setHeading
  }
}
object RandomController {

  val actorPath = "/user/randomController"

  def apply(system: ActorSystem, parent: ActorRef) = {
    system.actorOf(Props(classOf[RandomController], parent), "randomController")
  }
}
