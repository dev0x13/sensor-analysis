package net.bigdata.spark_analysis

import java.util

import scala.collection.JavaConversions._
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsync, AmazonDynamoDBAsyncClientBuilder}
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import net.liftweb.json._

class DynamoDBClient(
                      region: String,
                      awsAccessKey: String,
                      awsSecretKey: String
                    ) {
  val credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey)
  val credentialsProvider = new AWSStaticCredentialsProvider(credentials)


  val clientBuilder: AmazonDynamoDBAsyncClientBuilder = AmazonDynamoDBAsyncClientBuilder.standard()
  clientBuilder.setCredentials(credentialsProvider)
  clientBuilder.setRegion(region)

  val db: AmazonDynamoDBAsync = clientBuilder.build()

  def putItem(tableName: String, primaryKey: (String, String), stuff: util.Map[String, String], expirationTime: Long): Unit = {
    implicit val formats = DefaultFormats

    val item = new util.HashMap[String, AttributeValue]

    val keyAttribute = new AttributeValue()
    keyAttribute.setS(primaryKey._2)

    item.put(primaryKey._1, keyAttribute)

    for ((k, v) <- stuff) {
      val attr = new AttributeValue

      if (k.equals("timestamp")) {
        attr.setN(v)
      } else {
        attr.setS(v)
      }

      item.put(k, attr)
    }

    if (expirationTime != 0) {
      val attr = new AttributeValue()
      attr.setN(expirationTime.toString)
      item.put("expirationTime", attr)
    }

    db.putItemAsync(tableName, item)
  }

  def deleteItem(tableName: String, primaryKey: (String, String)): Unit = {
    val item = new util.HashMap[String, AttributeValue]

    val keyAttribute = new AttributeValue()
    keyAttribute.setS(primaryKey._2)

    item.put(primaryKey._1, keyAttribute)

    db.deleteItemAsync(tableName, item)
  }
}
