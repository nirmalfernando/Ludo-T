@echo off
setlocal EnableDelayedExpansion

set ROOT=%~dp0
set SRC=%ROOT%src
set TEST_SRC=%ROOT%test
set LIB=%ROOT%lib
set OUT=%ROOT%out

:: Build classpath from lib/
set CP=
for %%f in ("%LIB%\*.jar") do set CP=!CP!;%%f
set CP=!CP:~1!

:: Compile production sources if needed
if not exist "%OUT%\classes\com\ludot\Main.class" (
    echo Compiling production sources...
    if not exist "%OUT%\classes" mkdir "%OUT%\classes"
    for /r "%SRC%" %%f in (*.java) do echo %%f >> "%OUT%\src_files.tmp"
    javac -source 17 -target 17 -encoding UTF-8 -cp "!CP!" -d "%OUT%\classes" @"%OUT%\src_files.tmp"
    del "%OUT%\src_files.tmp"
    if errorlevel 1 ( echo Production compile failed. & exit /b 1 )
)

:: Compile test sources if needed
if not exist "%OUT%\test-classes" mkdir "%OUT%\test-classes"
echo Compiling test sources...
for /r "%TEST_SRC%" %%f in (*.java) do echo %%f >> "%OUT%\test_files.tmp"
javac -source 17 -target 17 -encoding UTF-8 ^
      -cp "%OUT%\classes;!CP!" ^
      -d "%OUT%\test-classes" ^
      @"%OUT%\test_files.tmp"
del "%OUT%\test_files.tmp"
if errorlevel 1 ( echo Test compile failed. & exit /b 1 )

:: Run tests
echo.
java -cp "%OUT%\test-classes;%OUT%\classes;!CP!" ^
     org.junit.platform.console.ConsoleLauncher ^
     --scan-class-path="%OUT%\test-classes" ^
     --fail-if-no-tests

endlocal
