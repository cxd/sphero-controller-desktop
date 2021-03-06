package au.id.cxd.sphero.controller.agents

import akka.actor.{Props, ActorSystem, ActorRef}
import au.id.cxd.math.probability.discrete.Binomial
import au.id.cxd.sphero.controller.RobotConfig
import au.id.cxd.sphero.controller.protocol.Update
import au.id.cxd.sphero.controller.state.model.State
import breeze.linalg.DenseVector
import org.slf4j.LoggerFactory
import se.nicklasgavelin.sphero.Robot
import se.nicklasgavelin.sphero.command.{SpinLeftCommand, RollCommand}

import scala.util.Random

/**
  * Created by cd on 24/12/2015.
  */
class BinomialCollisionController(parent: ActorRef) extends RobotController(parent) {

  val likelihoodLog = LoggerFactory.getLogger("likelihood")


  /**
    * we expect a threshold of 3 cm
    */
  val minDistanceThreshold = RobotConfig.doubleFor("robot.minDistanceThreshold", 1.0)

  /**
    * a vector of collision likelihood
    * each initialised to uniform prior.
    */
  var collisionLikelihood: DenseVector[Double] = DenseVector.ones[Double](360) * (1.0 / 360.0)

  /**
    * a vector of proportion parameter for binomial distribution
    * each initialised to uniform prior.
    */
  var proportion: DenseVector[Double] = DenseVector.ones[Double](360) * (1.0 / 360.0)


  /**
    * using jeffreys prior to allocate parameter alpha for the beta binomal distribution
    */
  var alphaParameters: DenseVector[Double] = DenseVector.ones[Double](360) * 0.5

  /**
    * jeffreys prior for beta parameters
    */
  var betaParameters: DenseVector[Double] = DenseVector.ones[Double](360) * 0.5


  /**
    * likelihood of reaching a collision within the last 10 moves
    */
  val slidingWindow = RobotConfig.intFor("robot.betaBinomial.slidingWindow", 10)

  /**
    * current N in sliding window
    */
  var currentN: DenseVector[Double] = DenseVector.zeros[Double](360)

  /**
    * current number of collisions in sliding window
    */
  var currentY: DenseVector[Double] = DenseVector.zeros[Double](360)

  /**
    * last selected heading
    *
    */
  var lastHeading: Int = 0

  override def receive = {

    case Update(robot) => {
      changeDirection(robot, state, false)
    }

    case any => super.receive(any)

  }

  def debugLikelihood(): Unit = {
    val debug = collisionLikelihood.toString()
    println(debug)

    val sb = collisionLikelihood.toArray.foldLeft(new StringBuilder()) {
      (sb:StringBuilder, item:Double) => {
        sb.append(s"$item,")
        sb
      }
    }
    val txt = sb.toString()

    likelihoodLog.info(txt.substring(0, txt.length - 1))

  }


  /**
    * change direction randomly
    * @param robot
    * @param state
    */
  def changeDirection(robot: Robot, state: State, wasCollision: Boolean): Unit = {

    val degrees = state.setHeading

    println(s"Degree: $degrees")

    val (heading, flag) = wasCollision match {
      case true => {
        updateCollision(degrees, collisionLikelihood)
        (selectLeastLikelihood(collisionLikelihood)(0, 359), wasCollision)
      }
      case _ => {
        val distance2d = state.distance(lastState)
        distance2d < minDistanceThreshold match {
          case true => {
            // determine if we have moved much from the previous state
            // we expect a step that is at least a magnitude of velocity along the heading
            println(s"Distance: $distance2d cm")
            updateCollision(degrees.toInt, collisionLikelihood)

            // use the current heading and select a minimum and maximum index 90 degrees
            // in the opposite direction of the current heading
            val theta = toRadian(degrees)
            val (min, max) = {
              val minDegree = {
                clamp( toDegree(Math.PI/4 + theta - toRadian(10)).toInt )
              }
              val maxDegree = {
                clamp( toDegree(Math.PI/4 + theta + toRadian(10)).toInt )
              }
              minDegree < maxDegree match {
                case true => (minDegree, maxDegree)
                case _ => minDegree == 0 || minDegree == 360 match {
                  case true => (maxDegree, 359)
                  case false => (maxDegree, minDegree)
                }
              }
            }
            (selectLeastLikelihood(collisionLikelihood)(min, max), true)
          }
          case false => (degrees, false)
        }
      }
    }

    state.setHeading = clamp(heading)

    val h = state.setHeading
    val h2 = state.lastSetHeading
    println(s"Final Heading: $h Last heading: $h2")
    flag match {
      case true => {
        // rotate to the heading before rolling
        // rotate first
        // execute the roll command 3 times.
        val command1 = new RollCommand(state.setHeading.toFloat, 1.0f, true)
        //val command1 = new SpinLeftCommand((constSpeed * 255).toInt)
        robot.sendCommand(command1)

      }
      case false => {}
    }

    val command = new RollCommand(state.setHeading.toFloat, constSpeed, false)
    robot.sendCommand(command)

    state.lastSetHeading = state.setHeading
  }


  /**
    * update the collision likelihood for the given state.
    * this is based only on the polar degree
    *
    * this is used to update the parameter vector for proportion
    * and the alpha and beta parameters for the degree heading
    *
    * @param likelihood
    */
  def updateCollision(degree: Int, likelihood: DenseVector[Double]) = {

    for (i <- 0 until likelihood.length) {

      val (n, y) = {
        val n = currentN.apply(i)
        val y = currentY.apply(i)
        i == degree match {
          case true => {
            if (n < slidingWindow) {
              currentN(degree) = n + 1
              currentY(degree) = y + 1
            } else {
              currentN(degree) = 1
              currentY(degree) = 1
            }
          }
          case false => {
            if (n < slidingWindow) {
              currentN(i) = n + 1
              //if (y > 0) {
              //  currentY(i) = y - 1
              //}
            } else {
              currentN(i) = 1
              if (currentY(i) > 0) {
                currentY(i) = 1
              }
            }
          }

        }

        (currentN.apply(i), currentY.apply(i))
      }
      // update the beta distribution parameters
      alphaParameters(i) = alphaParameters(i) + y
      betaParameters(i) = betaParameters(i) + n - y

      // compute most mean value for pi based on the beta parameters
      val pi = alphaParameters(i) / (alphaParameters(i) + betaParameters(i))
      proportion(i) = pi
      // calculate the likelihood of f(y|pi) using the updated parameter pi
      val binom = Binomial(slidingWindow.toDouble)(pi)
      likelihood(i) = binom.pdf(1)
    }
    likelihood
  }

  /**
    * select the angle (based on index) of the least likelihood of collision
    * step 3 degrees at each search
    * @param likelihood
    * @return
    */
  def selectLeastLikelihood(likelihood: DenseVector[Double])(min: Int, max: Int) = {
    def select(idx: Int, maxIdx: Int, matchIdx: Int, least: Double)(likelihood: DenseVector[Double]): (Int, Double) = {
      idx < likelihood.length match {
        case true => {
          idx >= min && idx <= maxIdx match {
            case true => least > likelihood(idx) match {
              case true => select(idx + 1, maxIdx, idx, likelihood(idx))(likelihood)
              case false => select(idx + 1, maxIdx, matchIdx, least)(likelihood)
            }
            case _ => (matchIdx, least)
          }

        }
        case _ => (matchIdx, least)
      }
    }

    debugLikelihood()
    println(s"Min:$min Max: $max")
    val (matchIdx, least) = select(min, max, min, Double.MaxValue)(likelihood)
    matchIdx
  }

}


object BinomialCollisionController {
  val actorPath = "user/binomController"

  def apply(system: ActorSystem, parent: ActorRef) = {
    system.actorOf(Props(classOf[BinomialCollisionController], parent), "binomController")
  }
}