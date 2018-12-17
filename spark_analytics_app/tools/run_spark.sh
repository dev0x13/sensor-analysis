#!/bin/bash
source vars.sh

$spark_root/sbin/start-master.sh
$spark_root/sbin/start-slave.sh -c 1 -m 1G $spark_addr

