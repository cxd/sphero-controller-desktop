package au.id.cxd.sphero.controller

import com.typesafe.config.{ConfigFactory, Config}

import scala.util.Try

/**
  * A configuration helper
  * Created by cd on 28/12/2015.
  */
object RobotConfig {

  val configName = "robot.conf"

  def load(config:String):Option[Config] = Try {
    ConfigFactory.load(config)
  } toOption

  def intFor(name:String, default:Int):Int = load(configName) flatMap {
    config => Try {
      config.getInt(name)
    } toOption
  } getOrElse default

  def doubleFor(name:String, default:Double):Double = load(configName) flatMap {
    config => Try {
      config.getDouble(name)
    } toOption
  } getOrElse default
}
