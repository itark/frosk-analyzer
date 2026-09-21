#!/usr/bin/env bash
# stop-all.command — stoppar equity :8080, crypto :8081, kraken-futures :8082 och frosk-dashboard

set -euo pipefail
cd "$(dirname "$0")"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'

log()  { echo -e "${GREEN}==>  $*${NC}"; }
warn() { echo -e "${YELLOW}[!]  $*${NC}"; }

kill_port() {
  local port=$1 name=$2
  local pids
  pids=$(lsof -ti tcp:"$port" 2>/dev/null || true)
  if [ -n "$pids" ]; then
    echo "$pids" | xargs kill -15 2>/dev/null || true
    sleep 1
    pids=$(lsof -ti tcp:"$port" 2>/dev/null || true)
    [ -n "$pids" ] && echo "$pids" | xargs kill -9 2>/dev/null || true
    log "Stoppade $name (:$port)"
  else
    warn "$name (:$port) körde inte"
  fi
}

kill_npm() {
  local pids
  pids=$(pgrep -f "react-scripts start\|next start\|vite\|frosk-dashboard" 2>/dev/null || true)
  if [ -n "$pids" ]; then
    echo "$pids" | xargs kill -15 2>/dev/null || true
    sleep 1
    pids=$(pgrep -f "react-scripts start\|next start\|vite\|frosk-dashboard" 2>/dev/null || true)
    [ -n "$pids" ] && echo "$pids" | xargs kill -9 2>/dev/null || true
    log "Stoppade frosk-dashboard"
  else
    warn "frosk-dashboard körde inte"
  fi
  for p in 3000 3001; do
    local pp
    pp=$(lsof -ti tcp:"$p" 2>/dev/null || true)
    [ -n "$pp" ] && echo "$pp" | xargs kill -9 2>/dev/null || true
  done
}

echo ""
echo "======================================"
echo "   frosk stop-all"
echo "======================================"
echo ""

kill_port 8080 "equity"
kill_port 8081 "crypto"
kill_port 8082 "kraken-futures"
kill_npm

echo ""
echo "======================================"
echo "   Klart"
echo "======================================"
echo ""

for port in 8080 8081 8082; do
  if lsof -ti tcp:"$port" &>/dev/null; then
    echo -e "${RED}[VARNING] Port $port fortfarande upptagen${NC}"
  else
    echo -e "${GREEN}[OK] :$port fri${NC}"
  fi
done
