package net.bigdata.spark_analysis

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.kinesis.AmazonKinesisClient
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.internal.Logging
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Milliseconds, StreamingContext}
import org.apache.spark.streaming.kinesis.KinesisInitialPositions.Latest
import org.apache.spark.streaming.kinesis.{KinesisInputDStream, SparkAWSCredentials}

object SensorJobExp extends Logging {
  def main(args: Array[String]) {
    Logger.getRootLogger.setLevel(Level.WARN)

    val kinesisAppName = "sensor-data-analyzer"
    val streamName = "SensorsData"
    val endpointUrl = "kinesis.us-east-2.amazonaws.com"

    val accessKey = "AKIAI7DA2HSJKOZ4I55Q"
    val secretKey = "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"
    val credentials1 = new SparkAWSCredentials
      .Builder()
      .basicCredentials(accessKey, secretKey)
      .build()

    val credentials = new DefaultAWSCredentialsProviderChain().getCredentials()
    require(credentials != null,
      "No AWS credentials found. Please specify credentials using one of the methods specified " +
        "in http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html")
    val kinesisClient = new AmazonKinesisClient(credentials)
    kinesisClient.setEndpoint(endpointUrl)
    val numShards = kinesisClient.describeStream(streamName).getStreamDescription().getShards().size

    val numStreams = numShards

    val batchInterval = Milliseconds(1000)

    val sparkConfig = new SparkConf().setAppName("SensorAnalysisWorker")
    val ssc = new StreamingContext(sparkConfig, batchInterval)

    val regionName = Regions.US_EAST_2.getName

    val kinesisStreams = (0 until numStreams).map { i =>
      KinesisInputDStream.builder
        .streamingContext(ssc)
        .kinesisCredentials(credentials1)
        .streamName(streamName)
        .endpointUrl(endpointUrl)
        .regionName(regionName)
        .initialPosition(new Latest())
        .checkpointAppName(kinesisAppName)
        .checkpointInterval(batchInterval)
        .storageLevel(StorageLevel.MEMORY_AND_DISK_2)
        .build()
    }

    val unionStreams = ssc.union(kinesisStreams)

    unionStreams.print()

    ssc.start()
    ssc.awaitTermination()
  }
}
