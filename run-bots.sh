#!/bin/bash
# Four bots, one per colour, into one room: ./run-bots.sh R-0001 [host] [port] [delay-ms]
cd "$(dirname "$0")"
ROOM=${1:?usage: ./run-bots.sh <room-id> [host] [port] [delay-ms]}
HOST=${2:-localhost}; PORT=${3:-5555}; DELAY=${4:-800}
for COLOUR in RED GREEN YELLOW BLUE; do
    ./run-bot.sh --host "$HOST" --port "$PORT" --room "$ROOM" --colour "$COLOUR" --delay "$DELAY" &
done
wait
