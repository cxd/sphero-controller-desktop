package au.id.cxd.sphero.controller.agents

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import au.id.cxd.sphero.controller._
import au.id.cxd.sphero.controller.protocol._
import au.id.cxd.sphero.controller.state.model.State
import breeze.linalg.DenseVector
import experimental.sensor.TouchSensor
import se.nicklasgavelin.sphero.RobotListener.EVENT_CODE
import se.nicklasgavelin.sphero.command._
import se.nicklasgavelin.sphero.response.information.{CollisiondetectedResponse, DataResponse}
import se.nicklasgavelin.sphero.response.parser.DataStreamParser
import se.nicklasgavelin.sphero.response.regular.ReadLocatorResponse
import se.nicklasgavelin.sphero.response.{InformationResponseMessage, ResponseMessage}
import se.nicklasgavelin.sphero.{Robot, RobotListener}

import scala.concurrent.duration._
import scala.util.{Random, Try}

/**
  * Created by cd on 11/09/2015.
  */
abstract class RobotController(parent: ActorRef)
  extends Actor with RobotControl with RobotListener
  with TouchSensor.TouchListener {

  import context.dispatcher


  var state: State = State(0, 0, 0, 0, 0, 0, 0, 0)
  var lastState: State = State(0, 0, 0, 0, 0, 0, 0, 0)

  override def preStart(): Unit = {

  }


  def receive = {
    case Connected(robot) => {
      val name = robot.getName
      println(s"Connected to $name")
      robots += ((name, robot))
      robot.addListener(this)

      context.system.scheduler.scheduleOnce(1000 millis, self, DeferredOp(robot, r => showConnected(r)))
      // wait a few seconds before we
      context.system.scheduler.scheduleOnce(1500 millis, self, InitSensors(robot))
      context.system.scheduler.scheduleOnce(2500 millis, self, Update(robot))
    }

    case InitSensors(robot) => {
      //context.system.scheduler.scheduleOnce(100 millis, self, DeferredOp(robot, r => startGyroStream(r)))

      context.system.scheduler.scheduleOnce(100 millis, self, DeferredOp(robot, r => startSensorDataStream(r)))

      context.system.scheduler.scheduleOnce(200 millis, self, DeferredOp(robot, r => detectCollision(r)))

    }

    case DeferredOp(robot, opFn) => {
      opFn(robot)
    }


    case Location(robot, response) => {
      // write the location data to csv
      locatorResponse(robot, response)
    }


    case Sleep(robot: Robot) => {
      sleep(robot, 1)
    }

    case Stop(robot) => {
      robot.stopMotors()
      robot.disconnect()
    }

    case other => {}

  }

  def event(robot: Robot, event_code: EVENT_CODE): Unit = {
    event_code match {
      case EVENT_CODE.CONNECTION_ESTABLISHED => ()
      case EVENT_CODE.CONNECTION_CLOSED_UNEXPECTED =>
        reconnect()
      case EVENT_CODE.MACRO_DONE => ()
      case EVENT_CODE.DISCONNECTED =>
        reconnect()
      case EVENT_CODE.CONNECTION_FAILED =>
        reconnect()
      case _ => ()
    }
  }

  def reconnect(): Unit = {
    println("Received connection closed will remove robots and search again")
    // remove all robots.
    robots.clear()
    // get the system to search again.
    context.system.scheduler.scheduleOnce(5000 millis, parent, NoConnection())
  }



  def informationResponseReceived(robot: Robot, response: InformationResponseMessage): Unit = {
    val seqNum = response.getMessageHeader.getSequenceNumber

    response match {
      case (dataResp: DataResponse) => {

        // note we are working only with the accelerometer stream
        val data = dataResp.getSensorData
        val parser = new DataStreamParser(sensor1Mask, sensor2Mask);
        val sensorData = parser.parse(data)

        // update the current state
        lastState = State.copy(state)

        val polar = toPolar(sensorData.getOdometer.getA, sensorData.getOdometer.getB)
        val degree = toDegree(polar._2).toInt

        state = State(sensorData.getOdometer.getA,
          sensorData.getOdometer.getB,
          sensorData.getAccelerometerRaw.getA,
          sensorData.getAccelerometerRaw.getB,
          sensorData.getAccelerometerRaw.getC,
          sensorData.getVelocity.getA,
          sensorData.getVelocity.getB,
          degree)


        val aX = state.accelX
        val aY = state.accelY
        val aZ = state.accelZ

        val x = state.x
        val y = state.y
        val xVel = state.speedX
        val yVel = state.speedY

        println(s"Accel ($aX,$aY,$aZ)")

        accelLog.info(s"$aX,$aY,$aZ")

        locationLog.info(s"$x,$y,$xVel,$yVel")
      }
    }

  }

  def responseReceived(robot: Robot, responseMessage: ResponseMessage, commandMessage: CommandMessage): Unit = {

    // TODO work out if respnse received indicates the completion of a command
    // then do next update cycle after command is processed.
    if (commandMessage != null) {
      commandMessage.getCommand match {
        case CommandMessage.COMMAND_MESSAGE_TYPE.ROLL =>
          context.system.scheduler.scheduleOnce(1500 millis, self, Update(robot));
        case CommandMessage.COMMAND_MESSAGE_TYPE.READ_LOCATOR =>
          Try {
            context.self ! Location(robot, responseMessage.asInstanceOf[ReadLocatorResponse])
          }
        case _ => ()
      }
    }

  }

  def touchEvent(robot: Robot): Unit = {

  }



  def stopStream(robot: Robot): Unit = {
    val command = new SetDataStreamingCommand(4, 1, SetDataStreamingCommand.DATA_STREAMING_MASKS.OFF, 1, 0)
    robot.sendCommand(command)
  }

  def collisionDetected(robot: Robot, response: CollisiondetectedResponse): Unit = {
    val axis = response.getAxis
    val x = response.getX
    val y = response.getY
    val z = response.getZ
    val copySt = State.copy(state)
    val posX = state.x
    val posY = state.y
    val posHeading = state.heading

    collisionLog.info(s"$axis,$x,$y,$z,$posX,$posY,$posHeading")
    context.system.scheduler.scheduleOnce(100 millis, self, DeferredOp(robot, r => changeDirection(r, copySt, true)))
  }

  def showConnected(robot:Robot):Unit = {
    robot.rgbTransition(255, 0, 0, 0, 255, 255, 50)
    robot.sendCommand(new FrontLEDCommand(1))
  }




}