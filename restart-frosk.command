#!/bin/bash
cd "$(dirname "$0")"
echo "==> Stoppar equity-instansen (port 8080)..."
lsof -ti tcp:8080 | xargs kill -9 2>/dev/null
sleep 2
echo "==> Laddar SDKMAN och sätter Java-version..."
export SDKMAN_DIR="$HOME/.sdkman"
source "$SDKMAN_DIR/bin/sdkman-init.sh"
sdk env
java --version
echo "==> Startar frosk-analyzer (equity)..."
exec mvn spring-boot:run
