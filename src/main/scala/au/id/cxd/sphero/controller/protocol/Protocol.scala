package au.id.cxd.sphero.controller.protocol

import se.nicklasgavelin.sphero.Robot
import se.nicklasgavelin.sphero.response.information.{CollisiondetectedResponse, DataResponse}
import se.nicklasgavelin.sphero.response.regular.ReadLocatorResponse

/**
 * Created by cd on 11/09/2015.
 */
class Protocol {

}

case class Search() extends Protocol {}
case class StopSearch() extends Protocol {}
case class NoConnection() extends Protocol {}
case class Connected(robot:Robot) extends Protocol {}
case class InitSensors(robot:Robot) extends Protocol {}
case class Update(robot:Robot) extends Protocol {}
case class Stop(robot:Robot) extends Protocol {}
case class Collision(robot:Robot, response:CollisiondetectedResponse) extends Protocol {}
case class Data(robot:Robot, response:DataResponse) extends Protocol {}
case class Exit() extends Protocol {}
case class Sleep(robot:Robot) extends Protocol {}
case class ReadLocation(robot:Robot) extends Protocol {}
case class Location(robot:Robot, response:ReadLocatorResponse) extends Protocol {}

case class DeferredOp(robot:Robot, opFn:Robot => Unit) extends Protocol {}


