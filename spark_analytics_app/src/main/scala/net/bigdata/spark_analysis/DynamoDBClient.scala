package net.bigdata.spark_analysis

import java.util

import scala.collection.JavaConversions._
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.amazonaws.services.dynamodbv2.document.internal.InternalUtils
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.google.gson.Gson

class DynamoDBClient(
                      region: String,
                      awsAccessKey: String,
                      awsSecretKey: String
                    ) {
  val credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey)
  val credentialsProvider = new AWSStaticCredentialsProvider(credentials)

  val clientBuilder: AmazonDynamoDBClientBuilder = AmazonDynamoDBClientBuilder.standard()
  clientBuilder.setCredentials(credentialsProvider)
  clientBuilder.setRegion(region)

  val db: AmazonDynamoDB = clientBuilder.build()
  val gson = new Gson()

  private def convertJsonStringToAttributeValue(jsonStr: String): util.Map[String, AttributeValue] = {
    val item = new Item().withJSON("document", jsonStr)
    val attributes = InternalUtils.toAttributeValues(item)
    attributes.get("document").getM
  }

  def putItem(tableName: String, primaryKey: (String, String), stuff: AnyRef, expirationTime: Long): Unit = {
    val putItemRequest = new PutItemRequest()
    val keyAttribute = new AttributeValue()

    keyAttribute.setS(primaryKey._2)

    putItemRequest.addItemEntry(primaryKey._1, keyAttribute)

    val attributeValues: util.Map[String, AttributeValue] = convertJsonStringToAttributeValue(gson.toJson(stuff))

    for ((k, v) <- attributeValues) {
      putItemRequest.addItemEntry(k, v)
    }

    if (expirationTime != 0) {
      val attr = new AttributeValue()
      attr.setN(expirationTime.toString)
      // FIXME
      putItemRequest.addItemEntry("timestamp", attr)
    }

    db.putItem(putItemRequest)
  }
}
