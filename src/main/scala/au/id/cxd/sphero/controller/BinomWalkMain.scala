package au.id.cxd.sphero.controller

import akka.actor.ActorSystem
import au.id.cxd.sphero.controller.system.{BinomialWalkTestSystem, RandomWalkTestSystem, SystemBuilder}

/**
  * Created by cd on 24/12/2015.
  */
object BinomWalkMain {

  def main(arg:Array[String]):Unit = {
    println("Starting system.")

    val runner = new SystemRunner with SystemBuilder {
      def build (system:ActorSystem) = BinomialWalkTestSystem(system)
    }

    val system = ActorSystem("RobotControllerSystem")
    runner.run(system)
  }

}
