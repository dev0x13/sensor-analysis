package net.bigdata.spark_analysis

import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.Duration
import org.apache.spark.streaming.kinesis.KinesisInitialPositions

/**
  * StreamingConfig with parameters for Kinesis.
  */
case class StreamConfig(
                         streamName:         String,
                         region:             String,
                         checkpointInterval: Duration,
                         initialPosition:    KinesisInitialPositions.Latest,
                         storageLevel:       StorageLevel,
                         appName:            String,
                         batchInterval:      Duration,
                         awsAccessKey:       String,
                         awsSecretKey:       String) {
  val endpointUrl = s"https://kinesis.$region.amazonaws.com"
}
