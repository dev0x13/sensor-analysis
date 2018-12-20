#!/bin/bash

version='1.0'

docker build \
    --build-arg version=$version \
    -t analytics-frontend:$version  \
    -f Dockerfile .

