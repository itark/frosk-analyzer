#!/bin/bash

./stop.sh

ARTIFACT="frosk-analyzer"
VERSION="0.4.2"
JAR_FILE="$ARTIFACT-$VERSION.jar"
TAG="test-tag"
TAG_URL="https://github.com/itark/frosk-analyzer/releases/download/$TAG/$ARTIFACT-$VERSION.jar"
BUILD_LIBS="/build/libs"

# jar-file target
cd $BUILD_LIBS

# Log-file
touch $ARTIFACT.log
LOG_FILE="$ARTIFACT.log"

# Check if the JAR file exists
if [ ! -f "$JAR_FILE" ]; then
  echo "JAR file not found at $JAR_FILE. Downloading..."
  wget $TAG_URL
fi

# Check if the JAR file exists
if [ ! -f "$JAR_FILE" ]; then
  echo "JAR file not found at $JAR_FILE"
  exit 1
fi

# Start the application
nohup java -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &
echo "JAR file started with nohup. Logs are being written to $LOG_FILE"
