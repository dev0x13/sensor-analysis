package net.bigdata.spark_analysis

import scala.collection.immutable

case class MotionPack(
                       username: String,
                       data: immutable.Map[String, immutable.Map[String, MotionEvent]]
                       )

case class MotionEvent(
                       label: String,
                       data: Array[Float]
                     )
