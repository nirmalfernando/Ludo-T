@echo off
setlocal EnableDelayedExpansion

set ROOT=%~dp0
set SRC=%ROOT%src
set LIB=%ROOT%lib
set OUT=%ROOT%out\production

:: Build classpath from lib/
set CP=
for %%f in ("%LIB%\*.jar") do set CP=!CP!;%%f
set CP=!CP:~1!

:: Compile if out\production is missing or empty
if not exist "%OUT%\com\ludot\Main.class" (
    echo Compiling sources...
    if not exist "%OUT%" mkdir "%OUT%"
    for /r "%SRC%" %%f in (*.java) do echo %%f >> "%OUT%\..\src_files.tmp"
    javac -source 17 -target 17 -encoding UTF-8 -cp "!CP!" -d "%OUT%" @"%OUT%\..\src_files.tmp"
    del "%OUT%\..\src_files.tmp"
    if errorlevel 1 (
        echo Compile failed.
        exit /b 1
    )
    echo Compile OK.
    echo.
)

:: Run the game, tee output to game_log.txt
powershell -Command "java -cp '%OUT%;!CP!' com.ludot.Main %* 2>&1 | Tee-Object -FilePath '%ROOT%game_log.txt'"
echo.
echo Output also saved to game_log.txt
endlocal
