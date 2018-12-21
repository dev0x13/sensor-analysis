package net.bigdata.spark_analysis

import java.util

import org.apache.spark.streaming.Duration

import scala.math.{pow, sqrt}
import scala.collection.immutable.HashMap

object UserState extends Enumeration {
  type UserState = Value
  val None = Value("None")
  val Walking = Value("Walking")
  val Standing = Value("Standing")
  val Sleeping = Value("Sleeping")
  val Sitting = Value("Sitting")
  val Calling = Value("Calling")
  val Chatting = Value("Chatting")
  val Away = Value("Away")
}

object DeviceState extends Enumeration {
  type DeviceState = Value
  val None = Value("None")
  val InPocket = Value("InPocket")
  val InHand = Value("InHand")
  val Idle = Value("Idle")
}

import UserState._
import DeviceState._

class CompoundState {
  var userState: UserState = UserState.None
  var deviceState: DeviceState = DeviceState.None
  var iterations: Long = 0
  var walkStartIter: Long = 0
  var stepCounter: Long = 0
  var sleepStartIter: Long = 0
  var typingIter: Long = 0
  var accelTrack: Array[Double] = Array(0, 0, 0)
  var eulerAngles: Array[Double] = Array(0, 0, 0)

  def mapRepr(): util.Map[String, String] = {
    val map = new util.HashMap[String, String]

    map.put("userState", userState.toString)
    map.put("deviceState", deviceState.toString)

    map
  }
}

class MotionAnalyzer(timeStep: Duration) {
  val posE = 0.03
  val lightE = 3
  val skipFrames = 5
  val standingT = 3500
  val sleepingT: Long = 3600 * 1000
  val accelTrackCoeffs = Array(1, 0.35, 0.15)
  val accelTrackCoeffsSum = 1.5
  val speakTangLim = Array(1.2, 1.75)
  val speakKrenLim = Array(0.2, 1.0)

  /* SENSORS:

    android.sensor.accelerometer_uncalibrated,
    com.samsung.sensor.light_ir,
    android.sensor.accelerometer,
    android.sensor.linear_acceleration,
    android.sensor.pressure,
    android.sensor.gyroscope_uncalibrated,
    android.sensor.game_rotation_vector,
    android.sensor.magnetic_field_uncalibrated,
    com.samsung.sensor.light_cct,
    android.sensor.gravity,
    android.sensor.magnetic_field,
    android.sensor.light,
    android.sensor.gyroscope,
    android.sensor.rotation_vector,
    android.sensor.orientation

    synth.sensor.display
   */

  val sensorTypes: HashMap[String, String] =
    HashMap(
      "light" -> "android.sensor.light",
      "rotation" -> "android.sensor.rotation_vector",
      "display" -> "synth.sensor.display",
      "stepCounter" -> "android.sensor.step_counter",
      "linAcceleration" -> "android.sensor.linear_acceleration",
      "proximity" -> "android.sensor.proximity"
    )

  private def unpackMotionEventData(motionPack: MotionPack, key: String): Array[Float] = {
    val d = motionPack.data.get(sensorTypes(key))

    if (d.isDefined) {
      val head1 = d.head

      if (head1.nonEmpty) {
        val head2 = head1.head

        return head2._2.data
      }
    }

    null
  }

  private def quatToEuler(q: Array[Float]): Array[Double] = {
    val eulerAngles: Array[Double] = Array(0, 0, 0)
    val sinr_cosp = +2.0 * (q(1) * q(1) + q(2) * q(3))
    val cosr_cosp = +1.0 - 2.0 * (q(1) * q(1) + q(2) * q(2))
    eulerAngles(0) = Math.atan2(sinr_cosp, cosr_cosp).toFloat
    // pitch (y-axis rotation)
    val sinp = +2.0 * (q(0) * q(2) - q(3) * q(1))
    if (Math.abs(sinp) >= 1) eulerAngles(1) = Math.copySign(Math.PI / 2, sinp).toFloat // use 90 degrees if out of range
    else eulerAngles(1) = Math.asin(sinp).toFloat
    // yaw (z-axis rotation)
    val siny_cosp = +2.0 * (q(0) * q(3) + q(1) * q(2))
    val cosy_cosp = +1.0 - 2.0 * (q(2) * q(2) + q(3) * q(3))
    eulerAngles(2) = Math.atan2(siny_cosp, cosy_cosp).toFloat

    eulerAngles
  }

  def processMotionPack(userState: CompoundState, motionPack: MotionPack): (String, CompoundState) = {
    userState.iterations += 1

    val rotation = unpackMotionEventData(motionPack, "rotation")
    val light = unpackMotionEventData(motionPack, "light")
    val display = unpackMotionEventData(motionPack, "display")

    val stepCounter = unpackMotionEventData(motionPack, "stepCounter")

    if (rotation != null) {
      val q = Array(rotation(3), rotation(0), rotation(1), rotation(2))
      userState.eulerAngles = quatToEuler(q)
    }

    if (rotation != null && sqrt(pow(rotation(0).toDouble, 2) +
                                 pow(rotation(1).toDouble, 2)) < posE) {
      userState.deviceState = Idle
      userState.userState = Away
      userState.sleepStartIter = userState.iterations
    } else {
      if (light != null && display != null &&
          light(0) < lightE && display(0) == 0) {
          userState.deviceState = InPocket
      } else {
        userState.deviceState = InHand
      }
    }

    if (userState.iterations < skipFrames) {
      if (stepCounter != null) {
        userState.stepCounter = stepCounter(0).toLong
      }

      val ret = (motionPack.username, userState)
      return ret
    }

    if (stepCounter != null && stepCounter(0) - userState.stepCounter > 1) {
      userState.userState = Walking
      userState.walkStartIter = userState.iterations
    } else {

      if ((userState.iterations - userState.walkStartIter) * timeStep.milliseconds > standingT &&
                  userState.deviceState != Idle) {
        if (userState.eulerAngles(0) > 0.165) {
          userState.userState = Standing
        } else {
          userState.userState = Sitting
        }
      }

      if ((userState.iterations - userState.sleepStartIter) * timeStep.milliseconds > sleepingT) {
        userState.userState = Sleeping
      }
    }

    val linAccel = unpackMotionEventData(motionPack, "linAcceleration")

    if (linAccel != null) {
      userState.accelTrack((userState.iterations % 3).toInt) =
        sqrt(linAccel(0) * linAccel(0) + linAccel(1) * linAccel(1) + linAccel(2) * linAccel(2))

      val accelZAver = (accelTrackCoeffs(0) * userState.accelTrack((userState.iterations % 3).toInt) +
        accelTrackCoeffs(1) * userState.accelTrack(((userState.iterations - 1) % 3).toInt) +
        accelTrackCoeffs(1) * userState.accelTrack(((userState.iterations - 2) % 3).toInt)) / accelTrackCoeffsSum

      if (accelZAver < 1.4f && accelZAver > 0.45f) {
        if (userState.typingIter < 10)
          userState.typingIter += 3
      } else {
        if (userState.typingIter > 0 )
          userState.typingIter -= 2
      }

      if (userState.typingIter >= 6 && userState.userState != Chatting) {
        userState.typingIter += 1
        userState.userState = Chatting
      }
    }

    if (display(0) == 0.0) {
      userState.sleepStartIter = userState.iterations
    }

    if (stepCounter != null) {
      userState.stepCounter = stepCounter(0).toLong
    }

    val proximity = unpackMotionEventData(motionPack, "proximity")

    if ((proximity == null || (proximity != null && proximity(0) < 1)) &&
        userState.eulerAngles(0) > speakTangLim(0) &&
        userState.eulerAngles(0) < speakTangLim(1) &&
        ((userState.eulerAngles(1) > speakKrenLim(0) && userState.eulerAngles(1) < speakKrenLim(1)) ||
         (userState.eulerAngles(1) > -speakKrenLim(1) && userState.eulerAngles(1) < -speakKrenLim(0)))) {
      userState.userState = Calling
    }

    (motionPack.username, userState)
  }
}
