#!/bin/bash
# Builds every module in dependency order. Each module is compiled with ONLY its declared
# dependencies on the classpath, so the compiler enforces the architecture (e.g. the client
# cannot see the game core). Classes go to out/main/<module>, test classes to out/test/<module>
# and runnable JARs to dist/<module>.jar.
set -euo pipefail
cd "$(dirname "$0")"

RELEASE=17
JFX_LIB="lib/javafx-sdk/lib"
mkdir -p out/main out/test dist

# build_module <name> <main-class|-> <dependency modules...>
build_module() {
    local name=$1 main=$2; shift 2
    local sources
    sources=$(find "src/$name" -name '*.java' 2>/dev/null || true)
    if [ -z "$sources" ]; then
        echo "  [$name] no sources yet, skipped"
        return
    fi

    local cp="" dep
    for dep in "$@"; do cp="$cp:out/main/$dep"; done
    cp="${cp#:}"

    local fx=()
    if [ "$name" = "client" ]; then
        fx=(--module-path "$JFX_LIB" --add-modules javafx.controls,javafx.graphics)
    fi

    rm -rf "out/main/$name" "out/test/$name"
    mkdir -p "out/main/$name" "out/test/$name"
    # shellcheck disable=SC2086
    javac --release $RELEASE -encoding UTF-8 -d "out/main/$name" ${fx[@]+"${fx[@]}"} ${cp:+-cp "$cp"} $sources

    if [ "$main" = "-" ]; then
        jar --create --file "dist/$name.jar" -C "out/main/$name" .
    else
        jar --create --file "dist/$name.jar" --main-class "$main" -C "out/main/$name" .
    fi

    local tests
    tests=$(find "test/$name" -name '*.java' 2>/dev/null || true)
    if [ -n "$tests" ]; then
        local tcp="out/main/$name"
        [ -n "$cp" ] && tcp="$tcp:$cp"
        # shellcheck disable=SC2086
        javac --release $RELEASE -encoding UTF-8 -d "out/test/$name" ${fx[@]+"${fx[@]}"} -cp "$tcp:lib/*" $tests
    fi
    echo "  [$name] built -> dist/$name.jar"
}

echo "Building modules..."
build_module core     com.ludot.Main
build_module protocol -
build_module server   com.ludot.server.ServerMain core protocol
build_module client   com.ludot.client.ClientApp  protocol
build_module bot      com.ludot.bot.BotMain       protocol
echo "Build complete."
