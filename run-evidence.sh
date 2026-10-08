#!/bin/bash
# Evidence run for the report and the demo (server 70+, client 70+, architecture 70+).
#
# Resets the database, starts the real server (backed by the database in config/server.properties), makes a
# fresh Play room and floods it with TWO stress clients that start at the same moment, while two GUI clients
# watch. It then checks the database holds exactly what the game played and writes everything to
# evidence/run-<time>/ : both stress reports, the server log (with queue depths), a thread dump, screenshots
# and a SUMMARY.txt.
#
#   ./run-evidence.sh                 default: 3000 requests per stress client, AI at 20 ms per move
#   REQUESTS=5000 FAST_MS=150 ./run-evidence.sh
#   GUI=0 ./run-evidence.sh           no GUI windows (headless)
#   CONFIG=other.properties ./run-evidence.sh
#
# Needs compile.sh run first and (for the GUI) the JavaFX SDK from download-tools.sh.
set -u
cd "$(dirname "$0")"

CONFIG=${CONFIG:-config/server.properties}
REQUESTS=${REQUESTS:-3000}
FAST_MS=${FAST_MS:-20}
GUI=${GUI:-1}
PORT=${PORT:-5555}
OUT=evidence/run-$(date +%Y%m%d-%H%M%S)
mkdir -p "$OUT"

# the working config holds the database password, so it lives outside the repo and is removed on exit
TMPCONF=$(mktemp /tmp/ludot-evidence.XXXXXX)
chmod 600 "$TMPCONF"
cp "$CONFIG" "$TMPCONF"
printf '\nserver.port=%s\nspeed.fastMs=%s\nlog.queueIntervalMs=100\n' "$PORT" "$FAST_MS" >> "$TMPCONF"

SERVER_PID=""
cleanup() {
    [ -n "$SERVER_PID" ] && kill "$SERVER_PID" 2>/dev/null
    rm -f "$TMPCONF"
}
trap cleanup EXIT

say() { echo "[$(date +%H:%M:%S)] $*"; }

{
    echo "Evidence run $(date)"
    echo "java: $(java -version 2>&1 | head -1)"
    echo "machine: $(uname -sm)"
    echo "database: $(grep -E '^db.mode=' "$TMPCONF" | tail -1), host $(grep -E '^db.url=' "$TMPCONF" | tail -1 | sed -E 's#.*//([^:/]+).*#\1#')"
    echo "requests per stress client: $REQUESTS, threads: 4, AI pacing at FAST: ${FAST_MS} ms"
} > "$OUT/environment.txt"
cat "$OUT/environment.txt"

say "Resetting the database to its initial state"
./run-dbtool.sh --config "$TMPCONF" reset > "$OUT/db-reset.txt" 2>&1 || { echo "DbTool reset failed, see $OUT/db-reset.txt"; exit 1; }
./run-dbtool.sh --config "$TMPCONF" status > "$OUT/db-roundtrip.txt" 2>&1

say "Starting the server"
./run-server.sh "$TMPCONF" > "$OUT/server.log" 2>&1 &
SERVER_PID=$!
for i in $(seq 1 60); do
    grep -q "listening on port" "$OUT/server.log" 2>/dev/null && break
    sleep 0.5
done
grep -q "listening on port" "$OUT/server.log" || { echo "Server did not start, see $OUT/server.log"; exit 1; }

# both stress clients begin sending at this moment, once both are seated
START=$(( $(date +%s) * 1000 + 15000 ))

say "Starting stressA (creates the room)"
./run-stress.sh --port "$PORT" --create --name stressA --seat RED --speed fast --start-at "$START" --start-game \
    --requests "$REQUESTS" --threads 4 --wait 900 > "$OUT/stressA.txt" 2>&1 &
PID_A=$!
ROOM=""
for i in $(seq 1 60); do
    ROOM=$(sed -n 's/^Created room \(R-[0-9]*\).*/\1/p' "$OUT/stressA.txt" | head -1)
    [ -n "$ROOM" ] && break
    sleep 0.5
done
[ -n "$ROOM" ] || { echo "stressA did not create a room, see $OUT/stressA.txt"; exit 1; }
say "Room is $ROOM; starting stressB"
./run-stress.sh --port "$PORT" --room "$ROOM" --name stressB --seat BLUE --start-at "$START" \
    --requests "$REQUESTS" --threads 4 --wait 900 > "$OUT/stressB.txt" 2>&1 &
PID_B=$!

if [ "$GUI" = "1" ]; then
    DELAY=$(( START - $(date +%s) * 1000 + 3000 ))
    say "Two GUI spectators will join and take a screenshot 3 s into the flood"
    for n in 1 2; do
        ./run-client.sh --port "$PORT" --connect --nickname "watcher$n" --join "$ROOM" \
            --screenshot "$OUT/spectator$n-during-flood.png" --screenshot-after "$DELAY" > /dev/null 2>&1 &
    done
fi

# a thread dump while the flood is running shows the named threads at work
NOW=$(( $(date +%s) * 1000 ))
WAIT=$(( (START + 4000 - NOW) / 1000 ))
[ "$WAIT" -gt 0 ] && sleep "$WAIT"
say "Taking a thread dump of the server"
jstack "$SERVER_PID" 2>/dev/null | grep -E '^"(acceptor|conn-reader|conn-writer|room-|lobby|scheduler|db-writer|link)' -A1 \
    | grep -v '^--' > "$OUT/threads.txt"

say "Waiting for the stress clients to finish (they wait for the game to end)"
wait "$PID_A"; STATUS_A=$?
wait "$PID_B"; STATUS_B=$?
say "stressA exit $STATUS_A, stressB exit $STATUS_B"

if [ "$GUI" = "1" ]; then
    say "Two fresh GUI spectators join the finished game for a final-state screenshot"
    for n in 1 2; do
        ./run-client.sh --port "$PORT" --connect --nickname "final$n" --join "$ROOM" \
            --screenshot "$OUT/spectator$n-final-state.png" --screenshot-after 5000 > /dev/null 2>&1 &
    done
    wait $(jobs -p | grep -v "^$SERVER_PID$") 2>/dev/null
fi

say "Checking the database (the writer drains asynchronously, so retry for a while)"
VERIFIED=1
for i in $(seq 1 60); do
    ./run-dbtool.sh --config "$TMPCONF" verify "$ROOM" > "$OUT/db-verify.txt" 2>&1 && { VERIFIED=0; break; }
    sleep 2
done
cat "$OUT/db-verify.txt"
./run-dbtool.sh --config "$TMPCONF" status > "$OUT/db-final-counts.txt" 2>&1

say "Stopping the server (it drains the database queue first)"
kill "$SERVER_PID" 2>/dev/null; wait "$SERVER_PID" 2>/dev/null; SERVER_PID=""

MAX_DEPTH=$(grep -o 'queueDepth=[0-9]*' "$OUT/server.log" | sed 's/queueDepth=//' | sort -n | tail -1)
DEPTH_LINES=$(grep -c 'queueDepth=' "$OUT/server.log")
{
    echo "EVIDENCE SUMMARY ($ROOM)"
    echo
    cat "$OUT/environment.txt"
    echo
    echo "Database round trip: $(sed -n 's/^Connected.*round trip takes about //p' "$OUT/db-roundtrip.txt")"
    echo
    for who in stressA stressB; do
        echo "--- $who"
        grep -E '^(sent=|UNEXPECTED|latency|chat:|updates seen|final stateHash)|=[0-9]+ ' "$OUT/$who.txt"
        echo
    done
    echo "--- server"
    echo "Room command queue: peak depth ${MAX_DEPTH:-0}, logged on $DEPTH_LINES lines (see server.log: it rises during the flood and returns to 0)"
    grep -c '^"room-' "$OUT/threads.txt" | sed 's/^/threads named room-*: /'
    grep -c '^"conn-reader' "$OUT/threads.txt" | sed 's/^/threads named conn-reader-*: /'
    grep -c '^"conn-writer' "$OUT/threads.txt" | sed 's/^/threads named conn-writer-*: /'
    echo
    echo "--- database"
    cat "$OUT/db-verify.txt"
    echo
    cat "$OUT/db-final-counts.txt"
} > "$OUT/SUMMARY.txt"

echo
cat "$OUT/SUMMARY.txt"
echo
say "Everything is in $OUT/"
[ "$STATUS_A" = "0" ] && [ "$STATUS_B" = "0" ] && [ "$VERIFIED" = "0" ]
