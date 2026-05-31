#!/bin/bash
set -e

mkdir -p out/production out/test

# Compile production code
javac -d out/production $(find src -name "*.java")

# Compile test code against production classes + lib JARs
javac -d out/test \
  -cp "out/production:lib/*" \
  $(find test -name "*.java")

echo "Build complete."
