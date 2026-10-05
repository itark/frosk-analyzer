#!/bin/bash
# Restart the frosk-analyzer KRAKEN-FUTURES instance (Kraken data, port 8082,
# ~/itark/froskH2DBKrakenFuturesFile). Only stops a previous kraken-futures
# instance — the equity (8080) and crypto (8081) processes are left alone.
# Modeled on start-crypto.command.

set -u

PROJECT_DIR="/Users/fredrikmoller/itark/git/frosk-analyzer"
cd "$PROJECT_DIR" || { echo "Could not cd to $PROJECT_DIR"; exit 1; }

# .command files run with the default system PATH, which does not include
# Homebrew — make sure mvn is findable regardless of how we were launched.
export PATH="/opt/homebrew/bin:$PATH"
if ! command -v mvn >/dev/null; then
  echo "mvn not found on PATH — install Maven (brew install maven)"; read -r -p "Press Enter to close..."; exit 1
fi

echo "==> Stopping any running KRAKEN-FUTURES instance (port 8082)..."
PORT_PID=$(lsof -ti tcp:8082 2>/dev/null)
if [ -n "$PORT_PID" ]; then
  echo "    Freeing port 8082 (PID $PORT_PID)"
  kill -9 $PORT_PID 2>/dev/null
else
  echo "    No process on port 8082."
fi

echo "==> Loading SDKMAN and activating project Java version (.sdkmanrc)..."
export SDKMAN_DIR="$HOME/.sdkman"
# sdkman-init.sh reads variables that may be unset (e.g. ZSH_VERSION) and
# would abort the script under `set -u` — relax it around the SDKMAN section.
set +u
if [ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
  # shellcheck disable=SC1091
  source "$HOME/.sdkman/bin/sdkman-init.sh"
else
  echo "    SDKMAN not found at $HOME/.sdkman — install from https://sdkman.io"
  exit 1
fi
sdk env
set -u

echo "==> Java version in use:"
java --version

echo "==> Starting frosk-analyzer KRAKEN-FUTURES with mvn spring-boot:run (profile: kraken-futures, port 8082)"
echo "    (Ctrl+C in this terminal to stop)"
echo
exec mvn spring-boot:run -Dspring-boot.run.profiles=kraken-futures
