package net.bigdata.spark_analysis

import com.amazonaws.regions.Regions
import org.apache.log4j.{Level, Logger}
import org.apache.spark.internal.Logging
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.kinesis.KinesisInitialPositions

object SensorJobScala extends Logging {
  def main(args: Array[String]) {
    Logger.getRootLogger.setLevel(Level.WARN)

    val checkpointInterval = Seconds(1)
    val batchInterval = Seconds(1)
    val initialPosition = new KinesisInitialPositions.Latest()
    val streamName = "SensorsData"
    val appName = "SensorAnalysisJob"

    val awsAccessKey = "AKIAI7DA2HSJKOZ4I55Q"
    val awsSecretKey = "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"

    val region = Regions.US_EAST_2.getName

    val storageLevel = StorageLevel.MEMORY_AND_DISK

    val streamConfig = StreamConfig(
      streamName,
      region,
      checkpointInterval,
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
