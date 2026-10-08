@echo off
rem Stress client: run-stress.bat (--room R-0001 ^| --create) [--host H] [--port P] [--name stressA] [--seat RED] [--requests 2000]
cd /d "%~dp0"
java -cp "dist\bot.jar;dist\protocol.jar" com.ludot.bot.StressMain %*
