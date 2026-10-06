@echo off
setlocal EnableDelayedExpansion
:: ============================================================
::  LUDO-T  --  Software Metrics Runner
::  Tools: JaCoCo, Checkstyle, PMD, SpotBugs
::  Usage:  run-metrics.bat
:: ============================================================

set ROOT=%~dp0
set SRC=%ROOT%src\core
set TEST_SRC=%ROOT%test\core
set LIB=%ROOT%lib
set TOOLS=%ROOT%tools
set OUT=%ROOT%out
set REPORT_DIR=%ROOT%reports

:: Guard: tools must be downloaded first
if not exist "%TOOLS%\jacocoagent.jar" (
    echo ERROR: Tools not found. Please run  download-tools.bat  first.
    exit /b 1
)

:: Clean build dirs
if exist "%OUT%" rmdir /s /q "%OUT%"
if exist "%REPORT_DIR%" rmdir /s /q "%REPORT_DIR%"
mkdir "%OUT%\classes"
mkdir "%OUT%\test-classes"
mkdir "%REPORT_DIR%\jacoco"
mkdir "%REPORT_DIR%\checkstyle"
mkdir "%REPORT_DIR%\pmd"
mkdir "%REPORT_DIR%\spotbugs"

:: Build classpath from lib/
set CP=
for %%f in ("%LIB%\*.jar") do set CP=!CP!;%%f
set CP=!CP:~1!

echo.
echo ============================================================
echo  Step 1/7  --  Compile production sources
echo ============================================================

for /r "%SRC%" %%f in (*.java) do echo %%f >> "%OUT%\src_files.txt"
javac -source 17 -target 17 -encoding UTF-8 ^
      -cp "!CP!" ^
      -d "%OUT%\classes" ^
      @"%OUT%\src_files.txt"
if errorlevel 1 ( echo COMPILE FAILED & exit /b 1 )
echo     OK.

echo.
echo ============================================================
echo  Step 2/7  --  Compile test sources
echo ============================================================
set TEST_CP=%OUT%\classes;!CP!
for /r "%TEST_SRC%" %%f in (*.java) do echo %%f >> "%OUT%\test_files.txt"
javac -source 17 -target 17 -encoding UTF-8 ^
      -cp "!TEST_CP!" ^
      -d "%OUT%\test-classes" ^
      @"%OUT%\test_files.txt"
if errorlevel 1 ( echo TEST COMPILE FAILED & exit /b 1 )
echo     OK.

echo.
echo ============================================================
echo  Step 3/7  --  [TOOL 1] JaCoCo  --  Run tests + coverage
echo ============================================================
set FULL_CP=%OUT%\test-classes;%OUT%\classes;!CP!
java -javaagent:"%TOOLS%\jacocoagent.jar=destfile=%OUT%\jacoco.exec,includes=com.ludot.*" ^
     -cp "!FULL_CP!" ^
     org.junit.platform.console.ConsoleLauncher ^
     --scan-class-path="%OUT%\test-classes" ^
     --fail-if-no-tests
if errorlevel 1 ( echo TESTS FAILED & exit /b 1 )

java -jar "%TOOLS%\jacococli.jar" report "%OUT%\jacoco.exec" ^
     --classfiles "%OUT%\classes" ^
     --sourcefiles "%SRC%" ^
     --html "%REPORT_DIR%\jacoco" ^
     --xml  "%REPORT_DIR%\jacoco\jacoco.xml" ^
     --csv  "%REPORT_DIR%\jacoco\jacoco.csv"
echo     JaCoCo HTML report --^> %REPORT_DIR%\jacoco\index.html

echo.
echo ============================================================
echo  Step 4/7  --  [TOOL 2] Checkstyle  --  Code style
echo ============================================================
java -jar "%TOOLS%\checkstyle.jar" ^
     -c "%ROOT%checkstyle.xml" ^
     -f xml ^
     -o "%REPORT_DIR%\checkstyle\checkstyle-src.xml" ^
     "%SRC%"
if errorlevel 1 echo     [WARN] Checkstyle violations found in src.

java -jar "%TOOLS%\checkstyle.jar" ^
     -c "%ROOT%checkstyle.xml" ^
     -f xml ^
     -o "%REPORT_DIR%\checkstyle\checkstyle-test.xml" ^
     "%TEST_SRC%"
if errorlevel 1 echo     [WARN] Checkstyle violations found in test.
echo     Checkstyle reports --^> %REPORT_DIR%\checkstyle\

echo.
echo ============================================================
echo  Step 5/7  --  [TOOL 3] PMD  --  Static analysis
echo ============================================================
call "%TOOLS%\pmd\bin\pmd.bat" check ^
     -d "%SRC%" ^
     -R "%ROOT%pmd-ruleset.xml" ^
     -f xml ^
     -r "%REPORT_DIR%\pmd\pmd-result.xml" ^
     --use-version java-17
if errorlevel 1 echo     [WARN] PMD violations found.
echo     PMD report         --^> %REPORT_DIR%\pmd\pmd-result.xml

echo.
echo ============================================================
echo  Step 6/7  --  [TOOL 4] SpotBugs  --  Bug pattern detection
echo ============================================================
java -jar "%TOOLS%\spotbugs\lib\spotbugs.jar" ^
     -textui ^
     -effort:max ^
     -medium ^
     -xml:withMessages ^
     -output "%REPORT_DIR%\spotbugs\spotbugs-result.xml" ^
     -auxclasspath "!CP!" ^
     "%OUT%\classes"
if errorlevel 1 echo     [WARN] SpotBugs issues found.
echo     SpotBugs report    --^> %REPORT_DIR%\spotbugs\spotbugs-result.xml

echo.
echo ============================================================
echo  Step 7/7  --  [TOOL 5] Cyclomatic Complexity
echo ============================================================
mkdir "%REPORT_DIR%\complexity"
java -jar "%TOOLS%\checkstyle.jar" ^
     -c "%ROOT%complexity.xml" ^
     -f xml ^
     -o "%REPORT_DIR%\complexity\complexity-result.xml" ^
     "%SRC%"
if errorlevel 1 echo     [WARN] Methods exceed complexity thresholds -- see report.
echo     Complexity report  --^> %REPORT_DIR%\complexity\complexity-result.xml

echo.
echo ============================================================
echo  All metrics complete.  Reports:
echo    reports\jacoco\index.html           (open in browser)
echo    reports\checkstyle\*.xml            (code style)
echo    reports\pmd\pmd-result.xml          (static analysis)
echo    reports\spotbugs\spotbugs-result.xml (bug patterns)
echo    reports\complexity\complexity-result.xml (cyclomatic complexity)
echo ============================================================
endlocal
