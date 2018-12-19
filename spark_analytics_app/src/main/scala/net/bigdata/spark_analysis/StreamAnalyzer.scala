package net.bigdata.spark_analysis

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.kinesis.AmazonKinesisClient
import net.liftweb.json._
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

    val rawStream = streamingSparkContext.union(sparkDStreams)

    val stringStream = rawStream.flatMap(data => new String(data).split("\n"))

    val motionStream = stringStream.map(data => {
      implicit val formats = DefaultFormats

      val json = parse(data)
      json.extract[MotionPack]
    })

    motionStream.foreachRDD(rdd => {
      println(rdd.count())
      rdd.foreach(motionPack => {
        val dynamoDBClient = new DynamoDBClient(
          config.region,
          config.awsAccessKey,
          config.awsSecretKey
        )

        val motionAnalyzer = new MotionAnalyzer(config.batchInterval)

        val userState = motionAnalyzer.processMotionPack(motionPack)
        val expirationTime = System.currentTimeMillis + 60 * 1000
        dynamoDBClient.putItem(
          "users_states",
          ("username", userState._1),
          userState._2,
          expirationTime)
      })
    })

    streamingSparkContext.start()
    streamingSparkContext.awaitTermination()
  }
}
