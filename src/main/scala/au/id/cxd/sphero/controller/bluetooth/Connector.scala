package au.id.cxd.sphero.controller.bluetooth

import java.util

import akka.actor.{Props, ActorSystem, ActorRef, Actor}
import au.id.cxd.sphero.controller.protocol.{StopSearch, Search, Connected, NoConnection}
import se.nicklasgavelin.bluetooth.Bluetooth
import se.nicklasgavelin.bluetooth.Bluetooth.EVENT
import se.nicklasgavelin.bluetooth.BluetoothDevice
import se.nicklasgavelin.bluetooth.BluetoothDiscoveryListener
import se.nicklasgavelin.sphero.Robot
import se.nicklasgavelin.sphero.RobotListener
import se.nicklasgavelin.sphero.command.CommandMessage
import se.nicklasgavelin.sphero.command.FrontLEDCommand
import se.nicklasgavelin.sphero.exception.InvalidRobotAddressException
import se.nicklasgavelin.sphero.exception.RobotBluetoothException
import se.nicklasgavelin.sphero.response.ResponseMessage
import se.nicklasgavelin.sphero.response.InformationResponseMessage

/**
 * Created by cd on 11/09/2015.
 */
class ConnectThread(parent:ActorRef) extends Actor with BluetoothDiscoveryListener with Runnable {
  var bt: Bluetooth = null
  var stopFlag: Boolean = false
  var robots: util.Collection[Robot] = new util.ArrayList[Robot]

  var responses:Int = 0


  def receive = {
    case Search() => {
      run
    }
    case StopSearch() => {
      stopThread
    }
  }

  /**
   * Stop everything regarding the connection and robots
   */
  private def stopThread() {
    if (bt != null) bt.cancelDiscovery
    this.stopFlag = true

    import scala.collection.JavaConversions._

    for (r <- robots) r.disconnect
    robots.clear
  }

  override def run() {
    try {
      bt = new Bluetooth(this, Bluetooth.SERIAL_COM)
      bt.discover
    }
    catch {
      case e: Exception => {
        e.printStackTrace
      }
    }
  }

  /*
   * *************************************
   * BLUETOOTH DISCOVERY STUFF
   */
  /**
   * Called when the device search is completed with detected devices
   *
   * @param devices The devices detected
   */
  def deviceSearchCompleted(devices: util.Collection[BluetoothDevice]) {
    System.out.println("Completed device discovery")

    import scala.collection.JavaConversions._

    for (d <- devices) {
      if (Robot.isValidDevice(d)) {
        System.out.println("Found robot " + d.getAddress)
        try {
          val r: Robot = new Robot(d)
          if (r.connect) {
            robots.add(r)
            System.out.println("Connected to " + d.getName + " : " + d.getAddress)
            parent ! Connected(r)
          }
          else {
            System.err.println("Failed to connect to robot")
            parent ! NoConnection()
          }
        }
        catch {
          case ex: InvalidRobotAddressException => {
            parent ! NoConnection()
            ex.printStackTrace
          }
          case ex: RobotBluetoothException => {
            parent ! NoConnection()
            ex.printStackTrace
          }
        }
      } else {
        parent ! NoConnection()
      }
    }
    devices.size() == 0 match {
      case true => parent ! NoConnection()
      case _ => ()
    }

  }

  /**
   * Called when the search is started
   */
  def deviceSearchStarted {
    System.out.println("Started device search")
  }

  /**
   * Called if something went wrong with the device search
   *
   * @param error The error that occurred
   */
  def deviceSearchFailed(error: Bluetooth.EVENT) {
    System.err.println("Failed with device search: " + error)
  }

  /**
   * Called when a Bluetooth device is discovered
   *
   * @param device The device discovered
   */
  def deviceDiscovered(device: BluetoothDevice) {
    System.out.println("Discovered device " + device.getName + " : " + device.getAddress)
  }


}

object ConnectThread {

  val actorPath = "/user/connectThread"

  def apply(system:ActorSystem, parent:ActorRef) = {
    val actor = system.actorOf(Props(classOf[ConnectThread], parent), "connectThread")
    actor
  }
}