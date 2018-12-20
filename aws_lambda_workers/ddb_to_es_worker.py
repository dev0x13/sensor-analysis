import boto3
from elasticsearch import Elasticsearch, RequestsHttpConnection
from requests_aws4auth import AWS4Auth

region = 'us-east-2'
service = 'es'
credentials = boto3.Session().get_credentials()
awsauth = AWS4Auth(credentials.access_key, credentials.secret_key, region, service, session_token=credentials.token)

host = 'search-bigdata-analytics-h2gqq7kopvhhi5rjzjpad6xe7e.us-east-2.es.amazonaws.com'

es = Elasticsearch(
        hosts=[{'host': host, 'port': 443}],
        http_auth=awsauth,
        use_ssl=True,
        verify_certs=True,
        connection_class=RequestsHttpConnection
        )

def lambda_handler(event, context):
    for record in event['Records']:
        es.index(index="user-states", doc_type='state', id=record['dynamodb']['Keys']['username']['S'], body=record["dynamodb"])
