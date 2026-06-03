@echo off
powershell -Command "java -cp 'out\production' com.ludot.Main %* 2>&1 | Tee-Object -FilePath game_log.txt"
echo.
echo Output also saved to game_log.txt
