#!/bin/bash
# One network bot: ./run-bot.sh --host H --port P --room R-0001 --colour RED [--delay 800] [--name bot_red]
cd "$(dirname "$0")"
exec java -cp "dist/bot.jar:dist/protocol.jar" com.ludot.bot.BotMain "$@"
