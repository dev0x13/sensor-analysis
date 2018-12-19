#!/bin/bash
source vars.sh

# Uncomment to enable remote debugger
export SPARK_SUBMIT_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=4000

$spark_root/bin/spark-submit \
    --class net.bigdata.spark_analysis.SensorJobScala \
    --master $spark_addr \
    ../build/libs/SensorAnalysisJob.jar
