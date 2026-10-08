#!/usr/bin/env bash
set -euo pipefail
echo "============================================================"
echo " LUDO-T  --  Downloading metrics tool JARs  (one-time)"
echo "============================================================"

mkdir -p tools

# ── 1. JaCoCo 0.8.11 ─────────────────────────────────────────
echo ""
echo "[1/5] JaCoCo 0.8.11 ..."
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
echo "[2/5] Checkstyle 10.14.2 ..."
[ -f tools/checkstyle.jar ] || {
    curl -sSL "https://github.com/checkstyle/checkstyle/releases/download/checkstyle-10.14.0/checkstyle-10.14.0-all.jar" -o tools/checkstyle.jar
    echo "    checkstyle.jar downloaded."
} && [ -f tools/checkstyle.jar ] && echo "    checkstyle.jar present."

# ── 3. PMD 7.3.0 ─────────────────────────────────────────────
echo ""
echo "[3/5] PMD 7.3.0 ..."
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
echo "[4/5] SpotBugs 4.8.6 ..."
if [ ! -d tools/spotbugs ]; then
    curl -sSL "https://github.com/spotbugs/spotbugs/releases/download/4.8.6/spotbugs-4.8.6.zip" -o tools/spotbugs.zip
    unzip -q tools/spotbugs.zip -d tools/spotbugs_tmp
    mv tools/spotbugs_tmp/spotbugs-4.8.6 tools/spotbugs
    rm -rf tools/spotbugs_tmp tools/spotbugs.zip
    echo "    SpotBugs extracted to tools/spotbugs/"
else
    echo "    tools/spotbugs/ already present."
fi

# ── 5. Runtime libraries for Assignment 2 (JDBC drivers + JavaFX SDK) ──
echo ""
echo "[5/5] Runtime libraries (PostgreSQL JDBC, H2, OpenJFX 21) ..."
mkdir -p lib
PG_VER=42.7.4
H2_VER=2.2.224
JFX_VER=21.0.5
[ -f lib/postgresql-$PG_VER.jar ] || {
    curl -sSL "https://repo1.maven.org/maven2/org/postgresql/postgresql/$PG_VER/postgresql-$PG_VER.jar" -o lib/postgresql-$PG_VER.jar
    echo "    postgresql-$PG_VER.jar downloaded."
}
[ -f lib/h2-$H2_VER.jar ] || {
    curl -sSL "https://repo1.maven.org/maven2/com/h2database/h2/$H2_VER/h2-$H2_VER.jar" -o lib/h2-$H2_VER.jar
    echo "    h2-$H2_VER.jar downloaded."
}
case "$(uname -s)-$(uname -m)" in
    Darwin-arm64)  JFX_PLATFORM=osx-aarch64 ;;
    Darwin-x86_64) JFX_PLATFORM=osx-x64 ;;
    Linux-aarch64) JFX_PLATFORM=linux-aarch64 ;;
    *)             JFX_PLATFORM=linux-x64 ;;
esac
if [ ! -d lib/javafx-sdk ]; then
    curl -sSL "https://download2.gluonhq.com/openjfx/$JFX_VER/openjfx-${JFX_VER}_${JFX_PLATFORM}_bin-sdk.zip" -o lib/javafx.zip
    unzip -q lib/javafx.zip -d lib/javafx_tmp
    mv lib/javafx_tmp/javafx-sdk-$JFX_VER lib/javafx-sdk
    rm -rf lib/javafx_tmp lib/javafx.zip
    echo "    OpenJFX $JFX_VER ($JFX_PLATFORM) extracted to lib/javafx-sdk/"
else
    echo "    lib/javafx-sdk/ already present."
fi

echo ""
echo "============================================================"
echo " All tools ready.  Run  ./run-metrics.sh  to analyse."
echo "============================================================"
