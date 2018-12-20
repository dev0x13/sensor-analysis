package net.bigdata.spark_analysis

import com.amazonaws.regions.Regions
import org.apache.log4j.{Level, Logger}
import org.apache.spark.internal.Logging
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Milliseconds, Seconds}
import org.apache.spark.streaming.kinesis.KinesisInitialPositions

object SensorJobScala extends Logging {
  def main(args: Array[String]) {
    Logger.getRootLogger.setLevel(Level.WARN)

    val batchInterval = Milliseconds(100)
    val initialPosition = new KinesisInitialPositions.Latest()
    val streamName = "SensorsData"
    val appName = "SensorAnalysisJob"

    val awsAccessKey = "AKIAJAHOQMLXFIJ4CUBQ"
    val awsSecretKey = "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"

    val region = Regions.US_EAST_2.getName

    val storageLevel = StorageLevel.MEMORY_AND_DISK

    val streamConfig = StreamConfig(
      streamName,
      region,
      initialPosition,
      storageLevel,
      appName,
      batchInterval,
      awsAccessKey,
      awsSecretKey
    )

    StreamAnalyzer.execute(streamConfig)
  }
}
