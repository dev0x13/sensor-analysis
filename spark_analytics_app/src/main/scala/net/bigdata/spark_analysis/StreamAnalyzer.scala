package net.bigdata.spark_analysis

import java.util.concurrent.TimeUnit

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.google.common.cache._
import net.liftweb.json._
import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.kinesis.{KinesisInputDStream, SparkAWSCredentials}
import scala.util.control.Breaks._


/*
class UsersStatesAccumulator(var usersStatesCache: Cache[String, CompoundState])
  extends AccumulatorV2[(String, CompoundState), Cache[String, CompoundState]] {

  def this() {
    this(CacheBuilder.newBuilder()
      .expireAfterAccess(30, TimeUnit.SECONDS)
      //.removalListener(removalListener)
      .build())
  }

  def add(v: (String, CompoundState)): Unit = {
    usersStatesCache.put(v._1, v._2)
  }

  def value: Cache[String, CompoundState] = {
    usersStatesCache
  }

  def reset(): Unit = {
    usersStatesCache.invalidateAll()
  }

  def isZero(): Boolean = {
    usersStatesCache.size() == 0
  }

  def copy(): AccumulatorV2[(String, CompoundState), Cache[String, CompoundState]] = {
    new UsersStatesAccumulator(usersStatesCache)
  }

  def merge(other: AccumulatorV2[(String, CompoundState), Cache[String, CompoundState]]) = {
    usersStatesCache.putAll(other.value.asMap())
  }
}
*/

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
        .initialPosition(config.initialPosition)
        .checkpointAppName(config.appName)
        .checkpointInterval(config.batchInterval)
        .storageLevel(StorageLevel.MEMORY_ONLY)
        .build()
    }

    val rawStream = streamingSparkContext.union(sparkDStreams)

    val stringStream = rawStream.flatMap(data => new String(data).split("\n"))

    /*
    val usersStatesAccumulator = new UsersStatesAccumulator()

    streamingSparkContext.sparkContext.register(usersStatesAccumulator)
*/

    val sharedDynamoDBClient = new DynamoDBClient(
      config.region,
      config.awsAccessKey,
      config.awsSecretKey
    )

    val userStatesCache: Cache[String, CompoundState] = CacheBuilder.newBuilder()
      .expireAfterWrite(15, TimeUnit.SECONDS)
      .removalListener(new RemovalListener[String, CompoundState] {
        def onRemoval(notification: RemovalNotification[String, CompoundState]): Unit = {
          if (notification.getCause == RemovalCause.EXPIRED) {
            val username = notification.getKey
            println("User offline: " + username)

            sharedDynamoDBClient.deleteItem(
              "users_states",
              ("username", username))
          }
        }
      })
      .build()

    val motionStream = stringStream.map(data => {
      implicit val formats = DefaultFormats

      val json = parse(data)
      json.extract[MotionPack]
    })

    val dynamoDBClient = new DynamoDBClient(
      config.region,
      config.awsAccessKey,
      config.awsSecretKey
    )

    motionStream.foreachRDD(rdd => {
      val motionPacks = rdd.collect()

      val motionAnalyzer = new MotionAnalyzer(config.batchInterval)

      breakable {
        for (motionPack <- motionPacks) {

          //var userState = usersStatesAccumulator.value.getIfPresent(motionPack.username)
          var userState = userStatesCache.getIfPresent(motionPack.username)

          if (System.currentTimeMillis - motionPack.data("synth.sensor.display").head._1.toLong > 7000) {
            break
          }

          if (userState == null) {
            userState = new CompoundState()
            println("New user online: " + motionPack.username)
          }

          val updUserState = motionAnalyzer.processMotionPack(userState, motionPack)

          //usersStatesAccumulator.add(updUserState)
          userStatesCache.put(updUserState._1, updUserState._2)

          val expirationTime = (System.currentTimeMillis / 1000) + 10

          val dataToStore = updUserState._2.mapRepr()

          dynamoDBClient.putItem(
            "users_states",
            ("username", updUserState._1),
            dataToStore, expirationTime)

          dataToStore.put("timestamp", System.currentTimeMillis.toString)

          dynamoDBClient.putItem(
            "users_states_log",
            ("username", updUserState._1),
            dataToStore, 0)
        }
      }
    })

    streamingSparkContext.start()
    streamingSparkContext.awaitTermination()
  }
}
