#!/bin/bash
cd "$(dirname "$0")"
echo "==> Stoppar equity-instansen (port 8080)..."
lsof -ti tcp:8080 | xargs kill -9 2>/dev/null
sleep 2
echo "==> Kontrollerar frosk-garch-service (port 8000)..."
if curl -s -o /dev/null -w "%{http_code}" --max-time 2 http://localhost:8000/health 2>/dev/null | grep -q "^200$"; then
  echo "    frosk-garch-service redan igång."
else
  echo "    Inte igång — startar..."
  (
    cd /Users/fredrikmoller/itark/git/frosk-garch-service
    source .venv/bin/activate
    nohup uvicorn main:app --port 8000 > /tmp/frosk-garch.log 2>&1 &
  )
  for i in $(seq 1 15); do
    if curl -s -o /dev/null -w "%{http_code}" --max-time 2 http://localhost:8000/health 2>/dev/null | grep -q "^200$"; then
      echo "    frosk-garch-service uppe."
      break
    fi
    sleep 1
  done
  if ! curl -s -o /dev/null -w "%{http_code}" --max-time 2 http://localhost:8000/health 2>/dev/null | grep -q "^200$"; then
    echo "    VARNING: frosk-garch-service svarar fortfarande inte efter 15s — se /tmp/frosk-garch.log."
    echo "    Fortsätter ändå: equity kör fail-closed (Regime.UNKNOWN, inga GARCH-gated entries) tills tjänsten är uppe."
  fi
fi
echo "==> Laddar SDKMAN och sätter Java-version..."
export SDKMAN_DIR="$HOME/.sdkman"
source "$SDKMAN_DIR/bin/sdkman-init.sh"
sdk env
java --version
echo "==> Startar frosk-analyzer (equity)..."
exec mvn spring-boot:run
