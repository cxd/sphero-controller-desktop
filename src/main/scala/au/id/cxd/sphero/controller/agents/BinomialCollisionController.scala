package au.id.cxd.sphero.controller.agents

import akka.actor.{Props, ActorSystem, ActorRef}
import au.id.cxd.math.probability.discrete.Binomial
import au.id.cxd.sphero.controller.RobotConfig
import au.id.cxd.sphero.controller.protocol.Update
import au.id.cxd.sphero.controller.state.model.State
import breeze.linalg.DenseVector
import se.nicklasgavelin.sphero.Robot
import se.nicklasgavelin.sphero.command.RollCommand

import scala.util.Random

/**
  * Created by cd on 24/12/2015.
  */
class BinomialCollisionController(parent: ActorRef) extends RobotController(parent) {

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
  val slidingWindow = 10.0

  /**
    * current N in sliding window
    */
  var currentN: DenseVector[Double] = DenseVector.zeros[Double](360)

  /**
    * current number of collisions in sliding window
    */
  var currentY: DenseVector[Double] = DenseVector.zeros[Double](360)

  override def receive = {

    case Update(robot) => {
      changeDirection(robot, state, false)
    }

    case any => super.receive(any)

  }


  /**
    * change direction randomly
    * @param robot
    * @param state
    */
  def changeDirection(robot: Robot, state: State, wasCollision: Boolean): Unit = {

    val heading = wasCollision match {
      case true => {
        updateCollision(state, collisionLikelihood)
        selectLeastLikelihood(collisionLikelihood)
      }
      case _ => state.heading
    }

    // determine if we have moved much from the previous state
    // we expect a step that is at least a magnitude of velocity along the heading
    val distance2d = state.distance(lastState)
    println(s"Distance: $distance2d cm")

    // check the threshold
    val heading2 = distance2d < minDistanceThreshold match {
      case true => {
        updateCollision(state, collisionLikelihood)
        randomHeading(robot, state, lastState, wasCollision)
      }
      case false => heading
    }

    val finalHeading = heading2 < 0 match {
      case true => {
        val tmp = Math.abs(heading2)
        360 - tmp
      }
      case false => heading2
    }


    println(s"Final Heading: $finalHeading")
    val command = new RollCommand(finalHeading.toFloat, constSpeed, false)
    robot.sendCommand(command)
  }


  /**
    * update the collision likelihood for the given state.
    * this is based only on the polar degree
    *
    * this is used to update the parameter vector for proportion
    * and the alpha and beta parameters for the degree heading
    *
    * @param state
    * @param likelihood
    */
  def updateCollision(state: State, likelihood: DenseVector[Double]) = {
    val polar = toPolar(state)
    val tmp = toDegree(polar._2).toInt

    val degree = tmp < 0 match {
      case false => tmp
      case true => {
        val tmp1 = Math.abs(tmp)
        360 - tmp1
      }
    }

    val n = currentN.apply(degree)
    val y = currentY.apply(degree)
    if (n < slidingWindow) {
      currentN(degree) = n + 1
      currentY(degree) = y + 1
    } else {
      currentN(degree) = 1
      currentY(degree) = 1
    }

    // we want to update g(pi|y) using alpha and beta
    // we use the update rules for the beta binomial
    // alpha = alpha + y
    // beta = beta + n - y
    // g(pi|a,b) \propto \pi ^ {\alpha + y - 1} (1 - \pi) ^ {\beta + n - y - 1}
    // for 0 \leq \pi \leq 1
    // the normalising constant for g(\pi|y) is
    // \frac{\Gamma(n + \alpha + \beta)} {\Gamma(y + \alpha)\Gamma(n - y + \beta)}

    // update the beta distribution parameters
    alphaParameters(degree) = alphaParameters(degree) + y
    betaParameters(degree) = betaParameters(degree) + n - y

    // compute most mean value for pi based on the beta parameters
    val pi = alphaParameters(degree) / (alphaParameters(degree) + betaParameters(degree))
    proportion(degree) = pi
    // calculate the likelihood of f(y|pi) using the updated parameter pi
    val binom = Binomial(slidingWindow.toDouble)(pi)
    likelihood(degree) = binom.pdf(1)
    likelihood
  }

  /**
    * select the angle (based on index) of the least likelihood of collision
    * @param likelihood
    * @return
    */
  def selectLeastLikelihood(likelihood: DenseVector[Double]) = {
    def select(idx: Int, matchIdx: Int, least: Double)(likelihood: DenseVector[Double]): (Int, Int, Double) = {
      idx < likelihood.length match {
        case true => least > likelihood(idx) match {
          case true => select(idx + 1, idx, likelihood(idx))(likelihood)
          case false => select(idx+1, matchIdx, least)(likelihood)
        }
        case _ => (idx, matchIdx, least)
      }
    }
    val (idx, matchIdx, least) = select(0, 0, Double.MaxValue)(likelihood)
    matchIdx
  }

}


object BinomialCollisionController {
  val actorPath = "user/binomController"

  def apply(system: ActorSystem, parent: ActorRef) = {
    system.actorOf(Props(classOf[BinomialCollisionController], parent), "binomController")
  }
}