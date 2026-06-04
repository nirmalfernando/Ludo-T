@echo off
setlocal
echo ============================================================
echo  LUDO-T  --  Downloading metrics tool JARs  (one-time)
echo ============================================================

if not exist tools mkdir tools

echo.
echo [1/4] JaCoCo 0.8.11 ...
if not exist tools\jacocoagent.jar (
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/jacoco/org.jacoco.agent/0.8.11/org.jacoco.agent-0.8.11-runtime.jar' -OutFile 'tools\jacocoagent.jar'"
    echo     jacocoagent.jar downloaded.
) else (
    echo     jacocoagent.jar already present.
)

if not exist tools\jacococli.jar (
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/jacoco/org.jacoco.cli/0.8.11/org.jacoco.cli-0.8.11-nodeps.jar' -OutFile 'tools\jacococli.jar'"
    echo     jacococli.jar downloaded.
) else (
    echo     jacococli.jar already present.
)

echo.
echo [2/4] Checkstyle 10.14.2 ...
if not exist tools\checkstyle.jar (
    powershell -Command "Invoke-WebRequest -Uri 'https://github.com/checkstyle/checkstyle/releases/download/checkstyle-10.14.0/checkstyle-10.14.0-all.jar' -OutFile 'tools\checkstyle.jar'"
    echo     checkstyle.jar downloaded.
) else (
    echo     checkstyle.jar already present.
)

echo.
echo [3/4] PMD 7.3.0 ...
if not exist tools\pmd (
    powershell -Command "Invoke-WebRequest -Uri 'https://github.com/pmd/pmd/releases/download/pmd_releases%%2F7.3.0/pmd-dist-7.3.0-bin.zip' -OutFile 'tools\pmd.zip'"
    powershell -Command "Expand-Archive -Path 'tools\pmd.zip' -DestinationPath 'tools\pmd_tmp' -Force"
    powershell -Command "Move-Item -Path 'tools\pmd_tmp\pmd-bin-7.3.0' -Destination 'tools\pmd'"
    powershell -Command "Remove-Item 'tools\pmd_tmp' -Recurse -Force -ErrorAction SilentlyContinue"
    del tools\pmd.zip
    echo     PMD extracted to tools\pmd\
) else (
    echo     tools\pmd\ already present.
)

echo.
echo [4/4] SpotBugs 4.8.6 ...
if not exist tools\spotbugs (
    powershell -Command "Invoke-WebRequest -Uri 'https://github.com/spotbugs/spotbugs/releases/download/4.8.6/spotbugs-4.8.6.zip' -OutFile 'tools\spotbugs.zip'"
    powershell -Command "Expand-Archive -Path 'tools\spotbugs.zip' -DestinationPath 'tools\spotbugs_tmp' -Force"
    powershell -Command "Move-Item -Path 'tools\spotbugs_tmp\spotbugs-4.8.6' -Destination 'tools\spotbugs'"
    powershell -Command "Remove-Item 'tools\spotbugs_tmp' -Recurse -Force -ErrorAction SilentlyContinue"
    del tools\spotbugs.zip
    echo     SpotBugs extracted to tools\spotbugs\
) else (
    echo     tools\spotbugs\ already present.
)

echo.
echo ============================================================
echo  All tools ready.  Run  run-metrics.bat  to analyse.
echo ============================================================
endlocal
