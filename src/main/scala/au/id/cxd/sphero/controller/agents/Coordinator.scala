package au.id.cxd.sphero.controller.agents

import akka.actor.{ActorSelection, ActorRef, Actor}
import au.id.cxd.sphero.controller.protocol._
import se.nicklasgavelin.sphero.Robot
import scala.concurrent.duration._

import scala.collection.mutable._

/**
  * Created by cd on 24/12/2015.
  */
trait Coordinator extends Actor {

  import context.dispatcher

  val robots: Map[String, Robot] = Map[String, Robot]()

  /**
    * abstract method to produce an actor ref
    * @return
    */
  def robotController():ActorSelection

  /**
    * access the connector thread
    * @return
    */
  def connectorThread():ActorSelection

  def receive = {
    case Connected(robot) => {
      val robo = robotController
      val id = robot.getId
      robots += (id -> robot)
      robo ! Connected(robot)
    }
    case NoConnection() => {
      println("Got no connection will search again in 5 seconds")
      context.system.scheduler.scheduleOnce(5000 millis, self, Search());
    }
    case Search() => {
      val connector = connectorThread
      connector ! Search()
    }
    case StopSearch() => {
      val connector = connectorThread
      connector ! StopSearch()
    }
    case Exit() => {
      val connector = connectorThread
      connector ! StopSearch()
      val robo = robotController
      robots.forall {
        pair => {
          robo ! Stop(pair._2)
          true
        }
      }
    }

  }


}
