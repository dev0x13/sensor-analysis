package net.bigdata.spark_analysis

import java.util.concurrent.TimeUnit

import com.google.common.cache.{Cache, CacheBuilder}
import org.apache.spark.streaming.Duration
import scala.math.{pow, sqrt}

import scala.collection.immutable.HashMap

class MotionAnalyzer(timeStep: Duration) {
  val posE = 0.03
  val lightE = 3
  val skipFrames = 5
  val standingT = 3500
  val sleepingT: Long = 3600 * 1000

  object UserState extends Enumeration {
    type UserState = Value
    val None, Walking, Standing, Sleeping = Value
  }

  object DeviceState extends Enumeration {
    type DeviceState = Value
    val None, InPocket, InHand, Idle = Value
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
  }

  lazy val userStatesCache: Cache[String, CompoundState] = CacheBuilder.newBuilder()
    .expireAfterAccess(30, TimeUnit.SECONDS)
    .build()

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
   */

  val sensorTypes: HashMap[String, String] =
    HashMap(
      "light" -> "android.sensor.light",
      "rotation" -> "android.sensor.rotation_vector",
      "display" -> "", // ????
      "stepCounter" -> "android.sensor.step_counter"
    )

  private def unpackMotionEventData(motionPack: MotionPack, key: String): Array[Float] = {
    motionPack.data.get(sensorTypes.get(key)).entrySet().iterator().next().getValue.data
  }

  def processMotionPack(motionPack: MotionPack): (String, CompoundState) = {
    /* One-sample analytics */

    var userState = userStatesCache.getIfPresent(motionPack.username)

    if (userState == null) {
      userState = new CompoundState()
    }

    val rotation = unpackMotionEventData(motionPack, "rotation")
    val light = unpackMotionEventData(motionPack, "light")
    val display = unpackMotionEventData(motionPack, "display")

    if (sqrt(pow(rotation(0).toDouble, 2) +
      pow(rotation(1).toDouble, 2)) < posE) {
      userState.deviceState = Idle
      userState.sleepStartIter = userState.iterations
    } else {
      if (light(0) < lightE && display(0) == 0) {
        userState.deviceState = InPocket
      } else {
        userState.deviceState = InHand
      }
    }

    val stepCounter = unpackMotionEventData(motionPack, "stepCounter")

    userState.stepCounter = stepCounter(0).toLong

    if (userState.iterations < skipFrames) {
      return (motionPack.username, userState)
    }

    if (stepCounter(0) - userState.stepCounter > 1) {
      userState.userState = Walking
      userState.walkStartIter = userState.iterations
    } else {
      if ((userState.iterations - userState.sleepStartIter) * timeStep.milliseconds > sleepingT) {
        userState.userState = Sleeping
      } else if ((userState.iterations - userState.walkStartIter) * timeStep.milliseconds > standingT) {
        userState.userState = Standing
      }
    }

    if (display(0) == 0.1) {
      userState.sleepStartIter = userState.iterations
    }

    (motionPack.username, userState)
  }
}
