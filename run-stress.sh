#!/bin/bash
# Stress client: ./run-stress.sh (--room R-0001 | --create) [--host H] [--port P] [--name stressA] [--seat RED]
#   [--requests 2000] [--threads 4] [--rate N] [--start-at epochMs] [--start-game] [--speed fast] [--wait 120]
cd "$(dirname "$0")"
exec java -cp "dist/bot.jar:dist/protocol.jar" com.ludot.bot.StressMain "$@"
