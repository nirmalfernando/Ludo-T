@echo off
rem Four bots into one room: run-bots.bat R-0001 [host] [port] [delay-ms]
cd /d "%~dp0"
set ROOM=%1
if "%ROOM%"=="" ( echo usage: run-bots.bat ^<room-id^> [host] [port] [delay-ms] & exit /b 1 )
set HOST=%2
if "%HOST%"=="" set HOST=localhost
set PORT=%3
if "%PORT%"=="" set PORT=5555
set DELAY=%4
if "%DELAY%"=="" set DELAY=800
for %%C in (RED GREEN YELLOW BLUE) do start "bot %%C" java -cp "dist\bot.jar;dist\protocol.jar" com.ludot.bot.BotMain --host %HOST% --port %PORT% --room %ROOM% --colour %%C --delay %DELAY%
