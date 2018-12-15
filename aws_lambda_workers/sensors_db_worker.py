from __future__ import print_function
import json
import boto3
import base64

def lambda_handler(event, context):
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table('sensors_data')

    for record in event['Records']:
      data = record["kinesis"]["data"]
      data = base64.b64decode(data).decode('utf8').replace("'", '"')
      print(data)
      data = json.loads(data)
      username = data["username"]
      data = data["data"]

      # compositeKey form: userId[sensorName][timestamp]
      for sensorName, sensorData in data.items():
          dataToPut = {}
          compositeKeyCommonPart = "%s[%s]" % (username, sensorName)
          for timestamp, sensorDataEntry in sensorData.items():
              dataToPut = {str(i):str(x) for i, x in enumerate(sensorDataEntry["data"])}
              dataToPut["label"] = sensorDataEntry["label"]
              dataToPut["compositeKey"] = "%s[%s]" % (compositeKeyCommonPart, timestamp)
              print(dataToPut)
              table.put_item(Item=dataToPut)
