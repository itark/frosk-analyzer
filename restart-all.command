#!/bin/bash
# ============================================================================
# restart-all.command
#
# Stops and restarts the full frosk stack in order: the two Python forecast
# microservices (garch :8000, prophet :8001) are checked/started first since
# equity's entry rules call out to them, then the three Spring Boot processes
# (equity :8080, crypto :8081, kraken-futures :8082) one at a time — waiting
# for each one's /actuator/health before starting the next, not in parallel —
# then frosk-dashboard (:3000).
#
# The equity/crypto/kraken-futures java processes are launched from the SAME
# working directory (this repo), so `mvn spring-boot:run` recompiles into the
# SAME target/classes for all three. Starting two of them concurrently races
# on that shared directory — confirmed in practice: one process reads a
# half-written .class file mid-recompile and dies with NoClassDefFoundError
# at startup. Waiting for each health check before launching the next is what
# avoids that, not a nice-to-have — do not parallelize these three.
#
# Everything is started with nohup + disown and backgrounded (not `exec`),
# unlike restart-frosk.command's single-instance equity-only script — this
# one needs to end up with four+ processes running, not hand the terminal to
# one of them.
# ============================================================================

cd "$(dirname "$0")" || exit 1
FROSK_DIR="$(pwd)"
DASHBOARD_DIR="/Users/fredrikmoller/itark/git/frosk-dashboard"
GARCH_DIR="/Users/fredrikmoller/itark/git/frosk-garch-service"
PROPHET_DIR="/Users/fredrikmoller/itark/git/frosk-prophet-service"
LOG_DIR="$FROSK_DIR/logs"
mkdir -p "$LOG_DIR"

# ── output helpers ──────────────────────────────────────────────────────────
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; BOLD='\033[1m'; NC='\033[0m'
ts()   { date '+%H:%M:%S'; }
info() { echo -e "$(ts) ℹ️  $1"; }
ok()   { echo -e "$(ts) ${GREEN}✅ $1${NC}"; }
warn() { echo -e "$(ts) ${YELLOW}⚠️  $1${NC}"; }
err()  { echo -e "$(ts) ${RED}❌ $1${NC}"; }
step() { echo; echo -e "${BLUE}${BOLD}==> $1${NC}"; }

# Plain indexed array (not associative — macOS ships bash 3.2 as /bin/bash,
# which has no associative arrays) collecting one summary line per service.
SUMMARY=()

# Polls $1 every 2s until it returns HTTP 200, up to $2 seconds (default 60).
# Returns 0 once healthy, 1 on timeout — never exits the script.
wait_for_http_200() {
    local url="$1" timeout="${2:-60}" waited=0 code
    while [ "$waited" -lt "$timeout" ]; do
        code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 "$url" 2>/dev/null)
        if [ "$code" = "200" ]; then
            return 0
        fi
        sleep 2
        waited=$((waited + 2))
    done
    return 1
}

kill_port() {
    local label="$1" port="$2"
    if lsof -ti "tcp:$port" >/dev/null 2>&1; then
        info "Stoppar $label (port $port)..."
        lsof -ti "tcp:$port" | xargs kill -9 2>/dev/null
    else
        info "$label (port $port) kördes inte."
    fi
}

echo -e "${BOLD}🐸 restart-all — startar om hela frosk-stacken${NC}"
echo "   $(ts) start"

# ── 1. Stop everything ──────────────────────────────────────────────────────
step "1/7 Stoppar körande instanser"
kill_port "equity"          8080
kill_port "crypto"          8081
kill_port "kraken-futures"  8082
kill_port "frosk-dashboard" 3000
kill_port "frosk-dashboard" 3001
sleep 2

# ── 2. Python forecast services ─────────────────────────────────────────────
# Same nohup + venv pattern as restart-frosk.command. Never killed above —
# these are idempotent long-running services shared across all three Java
# profiles; only started if not already answering /health.
start_python_service() {
    local name="$1" dir="$2" port="$3"
    if curl -s -o /dev/null -w "%{http_code}" --max-time 2 "http://localhost:$port/health" 2>/dev/null | grep -q "^200$"; then
        ok "$name (:$port) redan igång."
        SUMMARY+=("✅ $name (:$port) — redan igång")
        return
    fi
    if [ ! -d "$dir" ]; then
        err "$name: hittar inte $dir — hoppar över."
        SUMMARY+=("❌ $name (:$port) — katalog saknas ($dir)")
        return
    fi
    if [ ! -f "$dir/.venv/bin/activate" ]; then
        err "$name: ingen .venv i $dir — hoppar över. Se den repots README för setup."
        SUMMARY+=("❌ $name (:$port) — .venv saknas")
        return
    fi
    info "Startar $name..."
    (
        cd "$dir" || exit 1
        source .venv/bin/activate
        nohup uvicorn main:app --port "$port" > "/tmp/$name.log" 2>&1 &
        disown
    )
    if wait_for_http_200 "http://localhost:$port/health" 15; then
        ok "$name (:$port) uppe."
        SUMMARY+=("✅ $name (:$port) — UP")
    else
        warn "$name svarar fortfarande inte efter 15s — se /tmp/$name.log. Fortsätter ändå (fail-closed uppströms)."
        SUMMARY+=("⚠️  $name (:$port) — svarar inte, se /tmp/$name.log")
    fi
}

step "2/7 Kontrollerar Python-tjänster (garch :8000, prophet :8001)"
start_python_service "frosk-garch-service"   "$GARCH_DIR"   8000
start_python_service "frosk-prophet-service" "$PROPHET_DIR" 8001

# ── SDKMAN / Java ────────────────────────────────────────────────────────────
step "Laddar SDKMAN och sätter Java-version"
export SDKMAN_DIR="$HOME/.sdkman"
if [ -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]; then
    source "$SDKMAN_DIR/bin/sdkman-init.sh"
    sdk env
    java --version
else
    err "SDKMAN hittades inte på $SDKMAN_DIR — fortsätter med vilken java som redan är på PATH."
fi

# Starts one Spring Boot profile in the background, waits for its own health
# check before returning — the caller (below) relies on that to serialize the
# three mvn invocations and avoid the target/classes race described at the
# top of this file.
start_java_profile() {
    local name="$1" profile="$2" port="$3"
    step "Startar $name (:$port, profil $profile)"
    cd "$FROSK_DIR" || return
    nohup mvn spring-boot:run -Dspring-boot.run.profiles="$profile" > "$LOG_DIR/$profile.log" 2>&1 &
    disown
    info "mvn startad (pid $!) — loggar till $LOG_DIR/$profile.log, väntar på /actuator/health (max 60s)..."
    if wait_for_http_200 "http://localhost:$port/actuator/health" 60; then
        ok "$name (:$port) uppe."
        SUMMARY+=("✅ $name (:$port) — UP")
    else
        err "$name (:$port) svarar inte efter 60s — se $LOG_DIR/$profile.log."
        SUMMARY+=("❌ $name (:$port) — TIMEOUT, se $LOG_DIR/$profile.log")
    fi
}

# ── 3/4/5. equity → crypto → kraken-futures, strictly sequential ───────────
start_java_profile "equity"         equity         8080
start_java_profile "crypto"         crypto         8081
start_java_profile "kraken-futures" kraken-futures 8082

# ── 6. frosk-dashboard ───────────────────────────────────────────────────────
step "6/7 Startar frosk-dashboard (UI)"
if [ -d "$DASHBOARD_DIR" ]; then
    (
        cd "$DASHBOARD_DIR" || exit 1
        nohup npm start > "$LOG_DIR/dashboard.log" 2>&1 &
        disown
    )
    info "npm start startad — loggar till $LOG_DIR/dashboard.log, väntar på svar på :3000 (max 60s)..."
    if wait_for_http_200 "http://localhost:3000" 60; then
        ok "frosk-dashboard (:3000) uppe."
        SUMMARY+=("✅ frosk-dashboard (:3000) — UP")
    elif wait_for_http_200 "http://localhost:3001" 5; then
        # CRA auto-picks the next free port when 3000 is taken and the process
        # isn't attached to a TTY (nohup) — it won't prompt, it just moves on.
        ok "frosk-dashboard uppe på :3001 istället för :3000."
        SUMMARY+=("✅ frosk-dashboard (:3001) — UP (3000 var upptagen)")
    else
        err "frosk-dashboard svarar inte på :3000 eller :3001 efter 60s — se $LOG_DIR/dashboard.log."
        SUMMARY+=("❌ frosk-dashboard — TIMEOUT, se $LOG_DIR/dashboard.log")
    fi
else
    err "Hittar inte frosk-dashboard på $DASHBOARD_DIR — hoppar över."
    SUMMARY+=("❌ frosk-dashboard — katalog saknas ($DASHBOARD_DIR)")
fi

# ── 7. Summary ───────────────────────────────────────────────────────────────
step "7/7 Sammanfattning"
FAILED=0
for line in "${SUMMARY[@]}"; do
    echo "   $line"
    case "$line" in
        "❌"*) FAILED=$((FAILED + 1)) ;;
    esac
done
echo
if [ "$FAILED" -eq 0 ]; then
    echo -e "${GREEN}${BOLD}🎉 Allt uppe.${NC} $(ts)"
else
    echo -e "${YELLOW}${BOLD}⚠️  $FAILED tjänst(er) fick problem — se loggarna ovan.${NC} $(ts)"
fi
echo "Loggar: $LOG_DIR/*.log (Java + dashboard), /tmp/frosk-garch-service.log, /tmp/frosk-prophet-service.log"
