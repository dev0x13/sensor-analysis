package net.bigdata.spark_analysis

import java.nio.charset.StandardCharsets

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.google.gson.Gson
import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.kinesis.{KinesisInputDStream, SparkAWSCredentials}

object StreamAnalyzer {
  private def setupStreamingContext(sparkConf: SparkConf, config: StreamConfig): StreamingContext = {
    val ssc = new StreamingContext(sparkConf, config.batchInterval)
    ssc
  }

  def execute(config: StreamConfig): Unit = {
    val awsCredentials = new SparkAWSCredentials.Builder()
      .basicCredentials(config.awsAccessKey, config.awsSecretKey)
      .build()

    val cred = new BasicAWSCredentials(config.awsAccessKey, config.awsSecretKey)

    val kinesisClient = new AmazonKinesisClient(cred)

    val sparkConf = new SparkConf()
      .setAppName(config.appName)
      .setMaster("local[*]")

    val streamingSparkContext = setupStreamingContext(sparkConf, config)

    kinesisClient.setEndpoint(config.endpointUrl)

    val numShards = kinesisClient.describeStream(config.streamName).getStreamDescription.getShards.size

    val sparkDStreams = (0 until numShards).map { i =>
      KinesisInputDStream.builder
        .streamingContext(streamingSparkContext)
        .kinesisCredentials(awsCredentials)
        .cloudWatchCredentials(awsCredentials)
        .dynamoDBCredentials(awsCredentials)
        .streamName(config.streamName)
        .endpointUrl(config.endpointUrl)
        .regionName(config.region)
        .initialPosition(config.initialPosition)
        .checkpointAppName(config.appName)
        .checkpointInterval(config.batchInterval)
        .storageLevel(StorageLevel.MEMORY_ONLY)
        .build()
    }

    val unionStreams = streamingSparkContext.union(sparkDStreams)

    val usernames = unionStreams.map( data => {
      val gson = new Gson
      val json = new String(data)
      gson.fromJson(json, classOf[MotionPack])
    })

    usernames.print()

    streamingSparkContext.start()
    streamingSparkContext.awaitTermination()
  }
}
