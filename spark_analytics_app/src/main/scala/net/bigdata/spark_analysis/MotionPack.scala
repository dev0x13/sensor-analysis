package net.bigdata.spark_analysis

import scala.collection.mutable

case class MotionPack(
                       username: String,
                       data: mutable.Map[String, mutable.Map[String, MotionEvent]]
                       )

case class MotionEvent(
                       label: String,
                       data: Array[Float]
                     )
