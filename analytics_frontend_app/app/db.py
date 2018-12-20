# -*- coding: utf-8 -*-

from config import DefaultConfig

import boto3

class Db:
    def __init__(self):
        self.__dynamoDB = boto3.client(
            'dynamodb',
            region_name='us-east-2',
            aws_access_key_id=DefaultConfig.AWS_ACCESS_KEY,
            aws_secret_access_key=DefaultConfig.AWS_SECRET_KEY,
        )

    def getAllItems(self, table):
        return self.__dynamoDB.scan(TableName=table)
