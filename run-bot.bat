@echo off
rem One network bot: run-bot.bat --host H --port P --room R-0001 --colour RED [--delay 800] [--name bot_red]
cd /d "%~dp0"
java -cp "dist\bot.jar;dist\protocol.jar" com.ludot.bot.BotMain %*
