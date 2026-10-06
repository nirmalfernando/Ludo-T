#!/usr/bin/env bash
# ============================================================
#  LUDO-T Online  --  Software Metrics Runner (all modules)
#  Tools: JaCoCo, Checkstyle, PMD, SpotBugs, Cyclomatic Complexity
#  Usage:  ./run-metrics.sh        (run ./download-tools.sh once first)
#
#  Builds every module (core, protocol, server, client, bot), runs all of their
#  tests under JaCoCo into one coverage file, runs the four analysis tools over
#  every module's sources, and ends with a per-module summary table that is also
#  saved to reports/SUMMARY.txt.
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

MODULES="core protocol server client bot"
TEST_MODULES="$MODULES integration"
TOOLS="$ROOT/tools"
OUT="$ROOT/out"
REPORT_DIR="$ROOT/reports"
JFX_LIB="$ROOT/lib/javafx-sdk/lib"

# Guard: tools must be downloaded first
if [ ! -f "$TOOLS/jacocoagent.jar" ]; then
    echo "ERROR: Tools not found. Please run  ./download-tools.sh  first."
    exit 1
fi

# every library jar, listed out (the JUnit launcher does not expand lib/* the way java -cp does)
LIBS=$(printf '%s:' "$ROOT"/lib/*.jar)
LIBS=${LIBS%:}
FX_JARS=$(printf '%s:' "$JFX_LIB"/*.jar 2>/dev/null || true)
FX_JARS=${FX_JARS%:}

# the modules a test module may see (the same rule compile.sh enforces)
deps_of() {
    case "$1" in
        server) echo "core protocol" ;;
        client|bot) echo "protocol" ;;
        integration) echo "core protocol server bot" ;;
        *) echo "" ;;
    esac
}

# comma/colon/space joined lists of paths, one per module
src_dirs()   { for m in $MODULES; do printf '%s ' "$ROOT/src/$m"; done; }
test_dirs()  { for m in $TEST_MODULES; do [ -d "$ROOT/test/$m" ] && printf '%s ' "$ROOT/test/$m"; done; }
class_dirs() { for m in $MODULES; do [ -d "$OUT/main/$m" ] && printf '%s ' "$OUT/main/$m"; done; }
join_by()    { local sep=$1; shift; local first=1; for x in "$@"; do [ $first = 1 ] && first=0 || printf '%s' "$sep"; printf '%s' "$x"; done; }

rm -rf "$REPORT_DIR" "$OUT/jacoco.exec"
mkdir -p "$REPORT_DIR/jacoco" "$REPORT_DIR/checkstyle" "$REPORT_DIR/pmd" \
         "$REPORT_DIR/spotbugs" "$REPORT_DIR/complexity"

echo ""
echo "============================================================"
echo " Step 1/7  --  Build every module"
echo "============================================================"
"$ROOT/compile.sh"

echo ""
echo "============================================================"
echo " Step 2/7  --  [TOOL 1] JaCoCo  --  Run all tests + coverage"
echo "============================================================"
FAILED=0
for m in $TEST_MODULES; do
    [ -d "$OUT/test/$m" ] || continue
    cp="$OUT/test/$m:$OUT/main/$m"
    for d in $(deps_of "$m"); do cp="$cp:$OUT/main/$d"; done
    fx=()
    [ "$m" = "client" ] && fx=(--module-path "$JFX_LIB" --add-modules javafx.controls,javafx.graphics)
    echo "--- tests: $m"
    java ${fx[@]+"${fx[@]}"} \
        -javaagent:"$TOOLS/jacocoagent.jar=destfile=$OUT/jacoco.exec,append=true,includes=com.ludot.*" \
        -jar "$ROOT/lib/junit-platform-console-standalone-1.10.2.jar" \
        --class-path "$cp:$LIBS" --scan-class-path "$OUT/test/$m" \
        --details=summary > "$REPORT_DIR/tests-$m.txt" 2>&1 || FAILED=1
    grep -E "tests (successful|failed)" "$REPORT_DIR/tests-$m.txt" | tr -s ' ' | tr '\n' ' '
    echo
done
if [ "$FAILED" = "1" ]; then
    echo "SOME TESTS FAILED: see reports/tests-<module>.txt"
    exit 1
fi

CLASSFILES=(); for d in $(class_dirs); do CLASSFILES+=(--classfiles "$d"); done
SOURCEFILES=(); for d in $(src_dirs); do SOURCEFILES+=(--sourcefiles "$d"); done
java -jar "$TOOLS/jacococli.jar" report "$OUT/jacoco.exec" \
     "${CLASSFILES[@]}" "${SOURCEFILES[@]}" \
     --html "$REPORT_DIR/jacoco" \
     --xml  "$REPORT_DIR/jacoco/jacoco.xml" \
     --csv  "$REPORT_DIR/jacoco/jacoco.csv" > /dev/null
echo "    JaCoCo HTML report  -->  $REPORT_DIR/jacoco/index.html"

echo ""
echo "============================================================"
echo " Step 3/7  --  [TOOL 2] Checkstyle  --  Code style"
echo "============================================================"
# shellcheck disable=SC2046
java -jar "$TOOLS/checkstyle.jar" -c "$ROOT/checkstyle.xml" -f xml \
     -o "$REPORT_DIR/checkstyle/checkstyle-result.xml" $(src_dirs) || true
# shellcheck disable=SC2046
java -jar "$TOOLS/checkstyle.jar" -c "$ROOT/checkstyle.xml" -f xml \
     -o "$REPORT_DIR/checkstyle/checkstyle-test-result.xml" $(test_dirs) || true
echo "    Checkstyle report   -->  $REPORT_DIR/checkstyle/"

echo ""
echo "============================================================"
echo " Step 4/7  --  [TOOL 3] PMD  --  Static analysis"
echo "============================================================"
# shellcheck disable=SC2046
"$TOOLS/pmd/bin/pmd" check \
     -d "$(join_by , $(src_dirs))" \
     -R "$ROOT/pmd-ruleset.xml" \
     -f xml \
     -r "$REPORT_DIR/pmd/pmd-result.xml" \
     --use-version java-17 > /dev/null 2>&1 || true
echo "    PMD report          -->  $REPORT_DIR/pmd/pmd-result.xml"

echo ""
echo "============================================================"
echo " Step 5/7  --  [TOOL 4] SpotBugs  --  Bug pattern detection"
echo "============================================================"
AUX="$LIBS"
for d in $(class_dirs); do AUX="$AUX:$d"; done
[ -n "$FX_JARS" ] && AUX="$AUX:$FX_JARS"
# shellcheck disable=SC2046
java -jar "$TOOLS/spotbugs/lib/spotbugs.jar" \
     -textui \
     -effort:max \
     -medium \
     -xml:withMessages \
     -output "$REPORT_DIR/spotbugs/spotbugs-result.xml" \
     -auxclasspath "$AUX" \
     $(class_dirs) > /dev/null 2>&1 || true
echo "    SpotBugs report     -->  $REPORT_DIR/spotbugs/spotbugs-result.xml"

echo ""
echo "============================================================"
echo " Step 6/7  --  [TOOL 5] Cyclomatic Complexity"
echo "============================================================"
# shellcheck disable=SC2046
java -jar "$TOOLS/checkstyle.jar" -c "$ROOT/complexity.xml" -f xml \
     -o "$REPORT_DIR/complexity/complexity-result.xml" $(src_dirs) || true
echo "    Complexity report   -->  $REPORT_DIR/complexity/complexity-result.xml"

echo ""
echo "============================================================"
echo " Step 7/7  --  Per-module summary"
echo "============================================================"

# Findings per module. Checkstyle and PMD name a file path (.../src/<module>/...); SpotBugs names a class, whose
# package says which module it is in (com.ludot.server.* is server, and so on; everything else is core).
count_by_path() {   # $1 = xml report, $2 = element that marks one finding (error or violation)
    awk -v item="<$2 " '
        /<file name=/ { m = ""; if (match($0, /\/src\/[a-z]+\//)) m = substr($0, RSTART + 5, RLENGTH - 6) }
        index($0, item) && m != "" { n[m]++ }
        END { for (k in n) print k, n[k] }' "$1"
}
count_spotbugs() {
    awk '
        function module(cls) {
            if (cls ~ /^com\.ludot\.server/) return "server"
            if (cls ~ /^com\.ludot\.client/) return "client"
            if (cls ~ /^com\.ludot\.bot/) return "bot"
            if (cls ~ /^com\.ludot\.protocol/) return "protocol"
            return "core"
        }
        /<BugInstance / { pending = 1 }
        pending && match($0, /<Class classname="[^"]+"/) {
            cls = substr($0, RSTART + 18, RLENGTH - 19); n[module(cls)]++; pending = 0 }
        END { for (k in n) print k, n[k] }' "$1"
}
lookup() { awk -v key="$2" '$1 == key { print $2; found = 1 } END { if (!found) print 0 }' <<< "$1"; }

STYLE=$(count_by_path "$REPORT_DIR/checkstyle/checkstyle-result.xml" error)
PMD=$(count_by_path "$REPORT_DIR/pmd/pmd-result.xml" violation)
BUGS=$(count_spotbugs "$REPORT_DIR/spotbugs/spotbugs-result.xml")
COMPLEX=$(count_by_path "$REPORT_DIR/complexity/complexity-result.xml" error)

COVERAGE=$(awk -F, '
    function module(pkg) {
        if (pkg ~ /^com\.ludot\.server/) return "server"
        if (pkg ~ /^com\.ludot\.client/) return "client"
        if (pkg ~ /^com\.ludot\.bot/) return "bot"
        if (pkg ~ /^com\.ludot\.protocol/) return "protocol"
        return "core"
    }
    NR > 1 { m = module($2); lm[m] += $8; lc[m] += $9; bm[m] += $6; bc[m] += $7 }
    END { for (m in lc) {
            line = (lm[m] + lc[m]) ? 100 * lc[m] / (lm[m] + lc[m]) : 0
            br = (bm[m] + bc[m]) ? 100 * bc[m] / (bm[m] + bc[m]) : 0
            printf "%s %d %.0f %.0f\n", m, lm[m] + lc[m], line, br } }' "$REPORT_DIR/jacoco/jacoco.csv")

{
    printf '%-10s %7s %9s %9s | %10s %6s %9s %11s\n' Module Lines "Line cov" "Branch cov" Checkstyle PMD SpotBugs Complexity
    printf '%-10s %7s %9s %9s | %10s %6s %9s %11s\n' ---------- ------- --------- --------- ---------- ------ --------- -----------
    TOTAL_S=0; TOTAL_P=0; TOTAL_B=0; TOTAL_C=0
    for m in $MODULES; do
        row=$(awk -v key="$m" '$1 == key { printf "%s %s%% %s%%", $2, $3, $4 }' <<< "$COVERAGE")
        set -- $row
        s=$(lookup "$STYLE" "$m"); p=$(lookup "$PMD" "$m"); b=$(lookup "$BUGS" "$m"); c=$(lookup "$COMPLEX" "$m")
        printf '%-10s %7s %9s %9s | %10s %6s %9s %11s\n' "$m" "${1:--}" "${2:--}" "${3:--}" "$s" "$p" "$b" "$c"
        TOTAL_S=$((TOTAL_S + s)); TOTAL_P=$((TOTAL_P + p)); TOTAL_B=$((TOTAL_B + b)); TOTAL_C=$((TOTAL_C + c))
    done
    printf '%-10s %7s %9s %9s | %10s %6s %9s %11s\n' ALL "" "" "" "$TOTAL_S" "$TOTAL_P" "$TOTAL_B" "$TOTAL_C"
    echo
    echo "Coverage is the combined effect of every module's tests (including the cross-module integration tests)."
    echo "Complexity counts breaches of complexity.xml: cyclomatic complexity 10, NPath 200, 30 statements per method,"
    echo "class fan-out 35."
} | tee "$REPORT_DIR/SUMMARY.txt"

echo ""
echo "============================================================"
echo " All metrics complete.  Reports are in:  $REPORT_DIR/"
echo ""
echo "   reports/SUMMARY.txt                       (the table above)"
echo "   reports/jacoco/index.html                 (coverage; open in browser)"
echo "   reports/checkstyle/*.xml                  (code style)"
echo "   reports/pmd/pmd-result.xml                (static analysis)"
echo "   reports/spotbugs/spotbugs-result.xml      (bug patterns)"
echo "   reports/complexity/complexity-result.xml  (cyclomatic complexity)"
echo "   reports/tests-<module>.txt                (test results per module)"
echo "============================================================"
