#!/bin/bash
# Restart frosk-analyzer Spring Boot application.
# Stops any running instance, then starts a fresh one with the
# project's pinned Java version (via SDKMAN) and `mvn spring-boot:run`.

set -u

PROJECT_DIR="/Users/fredrikmoller/itark/git/frosk-analyzer"
cd "$PROJECT_DIR" || { echo "Could not cd to $PROJECT_DIR"; exit 1; }

echo "==> Stopping any running frosk-analyzer process..."
# Mirrors stop.sh — kill any java process whose command line contains 'frosk-analyzer'
PIDS=$(ps aux | grep '[j]ava' | grep 'frosk-analyzer' | awk '{print $2}')
if [ -n "$PIDS" ]; then
  echo "    Killing PIDs: $PIDS"
  kill -9 $PIDS 2>/dev/null
else
  echo "    No running frosk-analyzer process found."
fi

# Also try to free port 8080 in case something else is holding it
PORT_PID=$(lsof -ti tcp:8080 2>/dev/null)
if [ -n "$PORT_PID" ]; then
  echo "    Freeing port 8080 (PID $PORT_PID)"
  kill -9 $PORT_PID 2>/dev/null
fi

echo "==> Loading SDKMAN and activating project Java version (.sdkmanrc)..."
export SDKMAN_DIR="$HOME/.sdkman"
if [ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
  # shellcheck disable=SC1091
  source "$HOME/.sdkman/bin/sdkman-init.sh"
else
  echo "    SDKMAN not found at $HOME/.sdkman — install from https://sdkman.io"
  exit 1
fi
sdk env

echo "==> Java version in use:"
java --version

echo "==> Starting frosk-analyzer with mvn spring-boot:run"
echo "    (Ctrl+C in this terminal to stop)"
echo
exec mvn spring-boot:run
