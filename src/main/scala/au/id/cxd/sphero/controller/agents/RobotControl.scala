package au.id.cxd.sphero.controller.agents

import au.id.cxd.sphero.controller.RobotConfig
import au.id.cxd.sphero.controller.state.model.State
import org.slf4j.LoggerFactory
import se.nicklasgavelin.sphero.Robot
import se.nicklasgavelin.sphero.command._
import se.nicklasgavelin.sphero.response.regular.ReadLocatorResponse

import scala.collection.mutable
import scala.util.Random

/**
  * Created by cd on 24/12/2015.
  */
trait RobotControl {

  /**
    * logs for location, gyro, collision
    */
  val locationLog = LoggerFactory.getLogger("locator")
  val accelLog = LoggerFactory.getLogger("accel")
  val collisionLog = LoggerFactory.getLogger("collision")


  /**
    * base constant speed
    */
  val constSpeed = RobotConfig.doubleFor("robot.constSpeed", 0.2f).toFloat

  /**
    * X threshold for collision detection
    */
  val collisionXThreshold = RobotConfig.intFor("robot.collisionXThreshold", 80)

  /**
    * Y threshold for collision detection
    */
  val collisionYThreshold = RobotConfig.intFor("robot.collisionYThreshold", 80)

  /**
    * collection of robots registered via bluetooth
    */
  var robots: mutable.Map[String, Robot] = mutable.Map[String, Robot]()

  /**
    * sensor 1 masks will include accelerometer and gyro data
    */
  var sensor1Mask: Int = SetDataStreamingCommand.DATA_STREAMING_MASKS.ACCELEROMETER.X.RAW |
    SetDataStreamingCommand.DATA_STREAMING_MASKS.ACCELEROMETER.Y.RAW |
    SetDataStreamingCommand.DATA_STREAMING_MASKS.ACCELEROMETER.Z.RAW |
    SetDataStreamingCommand.DATA_STREAMING_MASKS.GYRO.X.RAW |
    SetDataStreamingCommand.DATA_STREAMING_MASKS.GYRO.Y.RAW |
    SetDataStreamingCommand.DATA_STREAMING_MASKS.GYRO.Z.RAW

  /**
    * sensor2 mask for odometer and velocity data
    */
  var sensor2Mask: Int = SetDataStreamingCommand.DATA_STREAMING_MASK2.ODOMETER.X |
    SetDataStreamingCommand.DATA_STREAMING_MASK2.ODOMETER.Y |
    SetDataStreamingCommand.DATA_STREAMING_MASK2.VELOCITY.X |
    SetDataStreamingCommand.DATA_STREAMING_MASK2.VELOCITY.Y

  /**
    * when receiving a locator response log out the information
    * @param robot
    * @param response
    */
  def locatorResponse(robot: Robot, response: ReadLocatorResponse) = {

    val x = response.getxPosition()
    val y = response.getyPosition()
    val xvel = response.getxVelocity()
    val yvel = response.getyVelocity()
    val speed = response.getSpeedOverGround
    println(s"Locator: $x,$y,$xvel,$yvel,$speed")
    locationLog.info(s"$x,$y,$xvel,$yvel,$speed")
  }


  /**
    * start sensor data stream
    * @param robot
    */
  def startSensorDataStream(robot: Robot): Unit = {
    // recommended data streaming settings based on locator API document
    // is N = 20 and M = 1
    // set last parameter to 0 for unlimited packet count
    val command =
      new SetDataStreamingCommand(20,
        1,
        sensor1Mask,
        0,
        sensor2Mask)
    robot.sendCommand(command)
  }

  /**
    * detect collisions
    * @param r
    */
  def detectCollision(r: Robot) {

    /**
      * set speed constant for collision detection.
      */
    val colSpeed = constSpeed * 255

    val msg: SetCollisionDetection =
      new SetCollisionDetection(SetCollisionDetection.COLLISION_DETECT_METHOD.XYZ_ACCELEROMETER,
        collisionXThreshold,
        collisionYThreshold,
        colSpeed.toInt,
        colSpeed.toInt,
        50)
    r.sendCommand(msg)
  }


  /**
    * change direction randomly
    * @param robot
    * @param state
    */
  def changeDirection(robot: Robot, state: State, wasCollision: Boolean): Unit


  /**
    * select a random heading
    * @param robot
    * @param state
    */
  def randomHeading(robot: Robot, state: State, lastState: State, wasCollision: Boolean): Float = {
    val x = state.x
    val y = state.y
    val x2 = lastState.x
    val y2 = lastState.y
    // this is the last angle.
    // rotate this angle 45 degrees
    val theta = toRadian(state.setHeading)

    val delta = Math.min(Math.abs(lastState.x - x), Math.abs(lastState.y - y))
    val angle =
      if (wasCollision) {
        theta * (180.0 / Math.PI) + 90
      } else {
        delta >= 100 match {
          case true => {
            theta * (180.0 / Math.PI) + 90
          }
          case false => {
            Random.nextDouble() * 2.0 * Math.PI * (180.0 / Math.PI) + 90
          }
        }
      }


    val angle2 = angle > 360 match {
      case true => angle - 360
      case false => angle
    }
    angle2.toFloat
  }

  /**
    * send the robot to sleep
    * @param robot
    * @param interval
    */
  def sleep(robot: Robot, interval: Int): Unit = {
    val command = new SleepCommand(interval)
    robot.sendCommand(command)
  }

  /**
    * set the robot to spin
    * @param robot
    * @param speed
    */
  def spin(robot: Robot, speed: Int): Unit = {
    val command = new SpinLeftCommand(speed)
    robot.sendCommand(command)
  }


  /**
    * degrees = radian (180/PI)
    *
    * radian = degrees (PI/180)
    *
    * This will modulo 360
    *
    * @param radian
    * @return
    */
  def toDegree(radian: Double):Double = {
    val tmp = (radian * (180.0 / Math.PI)) % 360
    tmp < 0 match {
      case false => tmp
      case true => {
        val tmp1 = Math.abs(tmp)
        360.0 - tmp1
      }
    }
  }

  /**
    * convert to radians
    * @param degree
    * @return
    */
  def toRadian(degree: Double) = degree * (Math.PI / 180.0)

  /**
    * calculate the angle between two cartesian coordinates
    *
    * len = \sqrt{a^2 + b^2}
    *
    * dot = |A||B|\cos\theta
    *
    * \cos\theta = \frac{1}{|A||B|}
    *
    * \theta = \acos {\frac{1}{|A||B|}}
    *
    * @param x1
    * @param y1
    * @param x2
    * @param y2
    * @return
    */
  def radiansBetween(x1:Int, y1:Int, x2:Int, y2:Int):Double = {
    val len1 = Math.sqrt(Math.pow(x1,2) + Math.pow(y1,2))
    val len2 = Math.sqrt(Math.pow(x2,2) + Math.pow(y2,2))
    val dot = x1*x2 + y1*y2
    val cosTheta = dot / (len1*len2)
    Math.acos(cosTheta)
  }


  /**
    * convert x and y to polar coordinates
    *
    * we navigate on the x y plane
    * @param x
    * @param y
    * @return
    */
  def toPolar(x: Int, y: Int):(Double, Double) = {
    val r = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2))
    // rotate this angle 45 degrees
    val theta = r > 0 match {
      case true => Math.atan(r / (1.0 * x))
      case false => {
        Random.nextDouble() * 2.0 * Math.PI
      }
    }
    (r, theta)
  }

  /**
    * convert x and y to polar coordinates
    *
    * we navigate on the x y plane
    * @param state
    * @return
    */
  def toPolar(state: State):(Double, Double) = {
    val x = state.x
    val y = state.y
    toPolar(x, y)
  }

  /**
    * clamp between 0 and 360 degrees
    * @param heading
    * @return
    */
  def clamp(heading:Int):Int = {
    heading < 0 match {
      case true => {
        val tmp = Math.abs(heading)
        360 - tmp
      }
      case false => heading
    }
  }
}
