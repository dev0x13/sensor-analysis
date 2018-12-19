package net.bigdata.spark_analysis

import java.util

import scala.collection.JavaConversions._
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsync, AmazonDynamoDBAsyncClientBuilder}
import com.amazonaws.services.dynamodbv2.document.internal.InternalUtils
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import net.liftweb.json._
import net.liftweb.json.Serialization.write

import scala.collection.mutable

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

  private def convertJsonStringToAttributeValue(jsonStr: String): util.Map[String, AttributeValue] = {
    val item = new Item().withJSON("document", jsonStr)
    val attributes = InternalUtils.toAttributeValues(item)
    attributes.get("document").getM
  }

  def putItem(tableName: String, primaryKey: (String, String), stuff: AnyRef, expirationTime: Long): Unit = {
    implicit val formats = DefaultFormats

    val item = new util.HashMap[String, AttributeValue]

    val keyAttribute = new AttributeValue()
    keyAttribute.setS(primaryKey._2)

    item.put(primaryKey._1, keyAttribute)

    val attributeValues: util.Map[String, AttributeValue] = convertJsonStringToAttributeValue(write(stuff))

    /*
    for ((k, v) <- attributeValues) {
      putItemRequest.addItemEntry(k, v)
    }
    */

    if (expirationTime != 0) {
      val attr = new AttributeValue()
      attr.setN(expirationTime.toString)
      item.put("expitationTime", attr)
    }

    db.putItemAsync(tableName, item)
  }
}
