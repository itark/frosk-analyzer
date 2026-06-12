#!/bin/bash
# Start the frosk-analyzer CRYPTO instance (Coinbase data, port 8081,
# ~/itark/froskH2DBCryptoFile). Unlike restart-frosk.command this only
# stops a previous crypto instance — the equity process on 8080 is left alone.

set -u

PROJECT_DIR="/Users/fredrikmoller/itark/git/frosk-analyzer"
cd "$PROJECT_DIR" || { echo "Could not cd to $PROJECT_DIR"; exit 1; }

# .command files run with the default system PATH, which does not include
# Homebrew — make sure mvn is findable regardless of how we were launched.
export PATH="/opt/homebrew/bin:$PATH"
if ! command -v mvn >/dev/null; then
  echo "mvn not found on PATH — install Maven (brew install maven)"; read -r -p "Press Enter to close..."; exit 1
fi

echo "==> Stopping any running CRYPTO instance (port 8081)..."
PORT_PID=$(lsof -ti tcp:8081 2>/dev/null)
if [ -n "$PORT_PID" ]; then
  echo "    Freeing port 8081 (PID $PORT_PID)"
  kill -9 $PORT_PID 2>/dev/null
else
  echo "    No process on port 8081."
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

echo "==> Starting frosk-analyzer CRYPTO with mvn spring-boot:run (profile: crypto, port 8081)"
echo "    (Ctrl+C in this terminal to stop)"
echo
exec mvn spring-boot:run -Dspring-boot.run.profiles=crypto
