import json
import boto3
import base64

def lambda_handler(event, context):
    dynamodb = boto3.client('dynamodb')

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
              if "data" in sensorDataEntry:
                  dataToPut["data"] = {"S": json.dumps(sensorDataEntry["data"])}
                  
              #if "label" in sensorDataEntry:
              #    dataToPut["label"] = {"S": sensorDataEntry["label"]}

              dataToPut["compositeKey"] = {"S": "%s[%s]" % (compositeKeyCommonPart, timestamp)}
              dataToPut["sensor"] = {"S": sensorName}
              dynamodb.put_item(TableName="sensor_data", Item=dataToPut)
       
