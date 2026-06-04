#!/usr/bin/env bash
set -euo pipefail
echo "============================================================"
echo " LUDO-T  --  Downloading metrics tool JARs  (one-time)"
echo "============================================================"

mkdir -p tools

# ── 1. JaCoCo 0.8.11 ─────────────────────────────────────────
echo ""
echo "[1/4] JaCoCo 0.8.11 ..."
[ -f tools/jacocoagent.jar ] || {
    curl -sSL "https://repo1.maven.org/maven2/org/jacoco/org.jacoco.agent/0.8.11/org.jacoco.agent-0.8.11-runtime.jar" -o tools/jacocoagent.jar
    echo "    jacocoagent.jar downloaded."
} && [ -f tools/jacocoagent.jar ] && echo "    jacocoagent.jar present."

[ -f tools/jacococli.jar ] || {
    curl -sSL "https://repo1.maven.org/maven2/org/jacoco/org.jacoco.cli/0.8.11/org.jacoco.cli-0.8.11-nodeps.jar" -o tools/jacococli.jar
    echo "    jacococli.jar downloaded."
} && [ -f tools/jacococli.jar ] && echo "    jacococli.jar present."

# ── 2. Checkstyle 10.14.2 ────────────────────────────────────
echo ""
echo "[2/4] Checkstyle 10.14.2 ..."
[ -f tools/checkstyle.jar ] || {
    curl -sSL "https://github.com/checkstyle/checkstyle/releases/download/checkstyle-10.14.0/checkstyle-10.14.0-all.jar" -o tools/checkstyle.jar
    echo "    checkstyle.jar downloaded."
} && [ -f tools/checkstyle.jar ] && echo "    checkstyle.jar present."

# ── 3. PMD 7.3.0 ─────────────────────────────────────────────
echo ""
echo "[3/4] PMD 7.3.0 ..."
if [ ! -d tools/pmd ]; then
    curl -sSL "https://github.com/pmd/pmd/releases/download/pmd_releases%2F7.3.0/pmd-dist-7.3.0-bin.zip" -o tools/pmd.zip
    unzip -q tools/pmd.zip -d tools/pmd_tmp
    mv tools/pmd_tmp/pmd-bin-7.3.0 tools/pmd
    rm -rf tools/pmd_tmp tools/pmd.zip
    echo "    PMD extracted to tools/pmd/"
else
    echo "    tools/pmd/ already present."
fi

# ── 4. SpotBugs 4.8.6 ────────────────────────────────────────
echo ""
echo "[4/4] SpotBugs 4.8.6 ..."
if [ ! -d tools/spotbugs ]; then
    curl -sSL "https://github.com/spotbugs/spotbugs/releases/download/4.8.6/spotbugs-4.8.6.zip" -o tools/spotbugs.zip
    unzip -q tools/spotbugs.zip -d tools/spotbugs_tmp
    mv tools/spotbugs_tmp/spotbugs-4.8.6 tools/spotbugs
    rm -rf tools/spotbugs_tmp tools/spotbugs.zip
    echo "    SpotBugs extracted to tools/spotbugs/"
else
    echo "    tools/spotbugs/ already present."
fi

echo ""
echo "============================================================"
echo " All tools ready.  Run  ./run-metrics.sh  to analyse."
echo "============================================================"
