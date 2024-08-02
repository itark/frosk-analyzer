#!/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd $SCRIPT_DIR

##########################################
# Stop applications
##########################################
kill -9 $(ps aux | grep java | grep frosk-analyzer | awk '{print $2}') 2>/dev/null
