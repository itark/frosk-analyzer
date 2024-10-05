#!/bin/bash

./stop.sh

ARTIFACT="frosk-analyzer"
VERSION="SNAPSHOT"
JAR_FILE="$ARTIFACT-$VERSION.jar"
TAG="test-tag"
TAG_URL="https://github.com/itark/frosk-analyzer/releases/download/$TAG/$ARTIFACT-$VERSION.jar"
BUILD_LIBS="build/libs"


echo "Java version:"
java --version


# jar-file target
cd $BUILD_LIBS
echo "Build libs:"
pwd

# Check if the JAR build-path exists
#if [ ! -d "$BUILD_LIBS" ]; then
#  echo "$BUILD_LIBS  not found!"
#  exit 1
#fi

# Log-file
touch $ARTIFACT.log
LOG_FILE="$ARTIFACT.log"

# Check if the JAR file exists
#if [ ! -f "$JAR_FILE" ]; then
#  echo "JAR file not found at $JAR_FILE. Downloading..."
wget -N $TAG_URL
#fi

# Check if the JAR file exists
if [ ! -f "$JAR_FILE" ]; then
  echo "JAR file not found at $JAR_FILE"
  exit 1
fi

# Start the application
nohup java -jar  -Dspring.config.name=$ARTIFACT -Dspring.config.location=./$ARTIFACT-$VERSION.properties  $JAR_FILE > $LOG_FILE 2>&1 &
echo "$BUILD_LIBS/$JAR_FILE started with nohup. Logs are being written to $BUILD_LIBS/$LOG_FILE"