package au.id.cxd.sphero.controller

import akka.actor._
import au.id.cxd.sphero.controller.protocol.Search
import au.id.cxd.sphero.controller.system.{RandomWalkTestSystem, SystemBuilder}

/**
 * Created by cd on 11/09/2015.
 */
object Main {

  def main(arg:Array[String]):Unit = {
    println("Starting system.")

    val runner = new SystemRunner with SystemBuilder {
      def build (system:ActorSystem) = RandomWalkTestSystem(system)
    }

    val system = ActorSystem("RobotControllerSystem")
    runner.run(system)
  }

}

