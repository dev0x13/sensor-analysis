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
  var accelerometerTrack: Array[Double] = Array(0, 0, 0)

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
      "accelerometer" -> "android.sensor.accelerometer"
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

  def processMotionPack(userState: CompoundState, motionPack: MotionPack): (String, CompoundState) = {
    /* One-sample analytics */

    userState.iterations += 1

    val rotation = unpackMotionEventData(motionPack, "rotation")
    val light = unpackMotionEventData(motionPack, "light")
    val display = unpackMotionEventData(motionPack, "display")

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

    val stepCounter = unpackMotionEventData(motionPack, "stepCounter")

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
      if ((userState.iterations - userState.sleepStartIter) * timeStep.milliseconds > sleepingT) {
        userState.userState = Sleeping
      } else if ((userState.iterations - userState.walkStartIter) * timeStep.milliseconds > standingT &&
                  userState.deviceState != Idle) {
        userState.userState = Standing
      }
    }

    if (display(0) == 0.1) {
      userState.sleepStartIter = userState.iterations
    }

    val accel = unpackMotionEventData(motionPack, "accelerometer")

    userState.accelerometerTrack((userState.iterations % 3).toInt) =
      sqrt(accel(1) * accel(1) + accel(2) * accel(2))

    val accelZAver = (accelTrackCoeffs(0) * accelTrackCoeffs((userState.iterations % 3).toInt) +
      accelTrackCoeffs(1) * accelTrackCoeffs(((userState.iterations - 1) % 3).toInt) +
      accelTrackCoeffs(1) * accelTrackCoeffs(((userState.iterations - 2) % 3).toInt)) / accelTrackCoeffsSum

    if (accelZAver < 0.3 && accelZAver > 0.063 && accel(0) < 1 && accel(1) < 0.7 && accel(2) < 0.6) {
      userState.userState = Chatting
    }

    if (stepCounter != null) {
      userState.stepCounter = stepCounter(0).toLong
    }

    val ret = (motionPack.username, userState)
    ret
  }
}
