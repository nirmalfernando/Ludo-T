#!/bin/bash
# Runs the JUnit tests of every module that has them. Run ./compile.sh first.
set -uo pipefail
cd "$(dirname "$0")"

# the JUnit launcher does not expand lib/* the way java -cp does, so list the jars out
LIBS=$(printf '%s:' lib/*.jar)
LIBS=${LIBS%:}

# run_tests <name> <dependency modules...>
run_tests() {
    local name=$1; shift
    [ -d "out/test/$name" ] && [ -n "$(find "out/test/$name" -name '*.class' | head -1)" ] || return 0
    local cp="out/test/$name:out/main/$name" dep
    for dep in "$@"; do cp="$cp:out/main/$dep"; done
    echo "=== tests: $name"
    local fx=()
    if [ "$name" = "client" ]; then
        fx=(--module-path lib/javafx-sdk/lib --add-modules javafx.controls,javafx.graphics)
    fi
    java ${fx[@]+"${fx[@]}"} -jar lib/junit-platform-console-standalone-1.10.2.jar \
        --class-path "$cp:$LIBS" \
        --scan-class-path "out/test/$name"
}

status=0
run_tests core || status=1
run_tests protocol || status=1
run_tests server core protocol || status=1
run_tests client protocol || status=1
run_tests bot protocol || status=1
run_tests integration core protocol server bot || status=1
exit $status
