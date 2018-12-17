#!/bin/bash
source vars.sh

$spark_root/sbin/stop-master.sh
$spark_root/sbin/stop-slave.sh
