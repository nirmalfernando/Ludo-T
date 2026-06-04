#!/usr/bin/env bash
# ============================================================
#  LUDO-T  --  Software Metrics Runner
#  Tools: JaCoCo · Checkstyle · PMD · SpotBugs
#  Usage:  ./run-metrics.sh
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC="$ROOT/src"
TEST="$ROOT/test"
LIB="$ROOT/lib"
TOOLS="$ROOT/tools"
OUT="$ROOT/out"
REPORT_DIR="$ROOT/reports"

# ── Guard: tools must be downloaded first ────────────────────
if [ ! -f "$TOOLS/jacocoagent.jar" ]; then
    echo "ERROR: Tools not found. Please run  ./download-tools.sh  first."
    exit 1
fi

# ── Helper: build colon-separated classpath from lib/ ────────
lib_cp() {
    local CP=""
    for jar in "$LIB"/*.jar; do CP="$CP:$jar"; done
    echo "${CP#:}"
}

# ── Clean build dirs ─────────────────────────────────────────
rm -rf "$OUT" "$REPORT_DIR"
mkdir -p "$OUT/classes" "$OUT/test-classes"
mkdir -p "$REPORT_DIR/jacoco" "$REPORT_DIR/checkstyle" \
         "$REPORT_DIR/pmd"    "$REPORT_DIR/spotbugs"

LIB_CP=$(lib_cp)

echo ""
echo "============================================================"
echo " Step 1 / 5  --  Compile production sources"
echo "============================================================"
find "$SRC" -name "*.java" | xargs javac \
    -source 17 -target 17 -encoding UTF-8 \
    -cp "$LIB_CP" \
    -d "$OUT/classes"
echo "    OK."

echo ""
echo "============================================================"
echo " Step 2 / 5  --  Compile test sources"
echo "============================================================"
find "$TEST" -name "*.java" | xargs javac \
    -source 17 -target 17 -encoding UTF-8 \
    -cp "$OUT/classes:$LIB_CP" \
    -d "$OUT/test-classes"
echo "    OK."

echo ""
echo "============================================================"
echo " Step 3 / 5  --  [1] JaCoCo  --  Run tests + collect coverage"
echo "============================================================"
java -javaagent:"$TOOLS/jacocoagent.jar=destfile=$OUT/jacoco.exec,includes=com.ludot.*" \
     -cp "$OUT/test-classes:$OUT/classes:$LIB_CP" \
     org.junit.platform.console.ConsoleLauncher \
     --scan-class-path="$OUT/test-classes" \
     --fail-if-no-tests

# Generate HTML + XML reports
java -jar "$TOOLS/jacococli.jar" report "$OUT/jacoco.exec" \
     --classfiles "$OUT/classes" \
     --sourcefiles "$SRC" \
     --html "$REPORT_DIR/jacoco" \
     --xml  "$REPORT_DIR/jacoco/jacoco.xml" \
     --csv  "$REPORT_DIR/jacoco/jacoco.csv"
echo "    JaCoCo HTML report  -->  $REPORT_DIR/jacoco/index.html"

echo ""
echo "============================================================"
echo " Step 4 / 5  --  [2] Checkstyle  --  Code style"
echo "============================================================"
java -jar "$TOOLS/checkstyle.jar" \
     -c "$ROOT/checkstyle.xml" \
     -f xml \
     -o "$REPORT_DIR/checkstyle/checkstyle-result.xml" \
     "$SRC" || true
java -jar "$TOOLS/checkstyle.jar" \
     -c "$ROOT/checkstyle.xml" \
     -f xml \
     -o "$REPORT_DIR/checkstyle/checkstyle-test-result.xml" \
     "$TEST" || true
echo "    Checkstyle report   -->  $REPORT_DIR/checkstyle/"

echo ""
echo "============================================================"
echo " Step 5 / 6  --  [3] PMD  --  Static analysis"
echo "============================================================"
"$TOOLS/pmd/bin/pmd" check \
     -d "$SRC" \
     -R "$ROOT/pmd-ruleset.xml" \
     -f xml \
     -r "$REPORT_DIR/pmd/pmd-result.xml" \
     --use-version java-17 || true
echo "    PMD report          -->  $REPORT_DIR/pmd/pmd-result.xml"

echo ""
echo "============================================================"
echo " Step 6 / 6  --  [4] SpotBugs  --  Bug pattern detection"
echo "============================================================"
java -jar "$TOOLS/spotbugs/lib/spotbugs.jar" \
     -textui \
     -effort:max \
     -medium \
     -xml:withMessages \
     -output "$REPORT_DIR/spotbugs/spotbugs-result.xml" \
     -auxclasspath "$LIB_CP" \
     "$OUT/classes" || true
echo "    SpotBugs report     -->  $REPORT_DIR/spotbugs/spotbugs-result.xml"

echo ""
echo "============================================================"
echo " All metrics complete.  Reports are in:  $REPORT_DIR/"
echo ""
echo "   reports/jacoco/index.html           (open in browser)"
echo "   reports/checkstyle/*.xml"
echo "   reports/pmd/pmd-result.xml"
echo "   reports/spotbugs/spotbugs-result.xml"
echo "============================================================"
