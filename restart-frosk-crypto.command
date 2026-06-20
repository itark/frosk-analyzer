#!/bin/bash
cd "$(dirname "$0")"
echo "==> Stoppar crypto-instansen (port 8081)..."
lsof -ti tcp:8081 | xargs kill -9 2>/dev/null
sleep 2
echo "==> Laddar SDKMAN och sätter Java-version..."
export SDKMAN_DIR="$HOME/.sdkman"
source "$SDKMAN_DIR/bin/sdkman-init.sh"
sdk env
java --version
echo "==> Startar frosk-analyzer (crypto)..."
exec mvn spring-boot:run -Dspring-boot.run.profiles=crypto
