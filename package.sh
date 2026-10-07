#!/bin/bash
# Builds a clean submission zip: ./package.sh [output.zip]
# Leaves out secrets (config/server.properties), local databases, build output, downloaded libraries and tools,
# then unpacks the zip into a temporary folder and checks that it compiles, so a missing file shows up here.
set -euo pipefail
cd "$(dirname "$0")"
NAME=Ludo-T
OUT="${1:-../${NAME}-submission.zip}"
case "$OUT" in /*) ;; *) OUT="$PWD/$OUT" ;; esac
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

mkdir "$WORK/$NAME"
rsync -a \
  --exclude='.git' --exclude='.DS_Store' \
  --exclude='/out' --exclude='/dist' --exclude='/reports' --exclude='/h2data' --exclude='/evidence/*/tmp' \
  --exclude='/config/server.properties' --exclude='/game_log.txt' \
  --exclude='/lib/javafx-sdk' --exclude='/lib/*.jar' \
  --exclude='/tools/*.jar' --exclude='/tools/pmd' --exclude='/tools/spotbugs' --exclude='/tools/*.zip' \
  ./ "$WORK/$NAME/"

# the real password must not appear anywhere in the package (it is read here and never printed)
if [ -f config/server.properties ]; then
  SECRET=$(sed -n 's/^ *db\.password *= *//p' config/server.properties | head -1)
  if [ -n "$SECRET" ] && grep -rIlF --exclude-dir=.git -- "$SECRET" "$WORK/$NAME" >/dev/null; then
    echo "The database password appears in a file of the package: stopping." >&2
    grep -rIlF -- "$SECRET" "$WORK/$NAME" | sed "s|$WORK/||" >&2
    exit 1
  fi
fi

( cd "$WORK" && rm -f "$OUT" && zip -qr "$OUT" "$NAME" )
echo "Package written: $OUT ($(du -h "$OUT" | cut -f1))"

# Check: the unpacked copy compiles when the libraries are restored (libraries are not part of the zip)
CHECK=$(mktemp -d)
trap 'rm -rf "$WORK" "$CHECK"' EXIT
unzip -q "$OUT" -d "$CHECK"
cp -R lib "$CHECK/$NAME/" 2>/dev/null || true
( cd "$CHECK/$NAME" && ./compile.sh > /dev/null ) && echo "Check passed: the unpacked copy compiles."
echo "Contents without secrets: $(unzip -l "$OUT" | grep -c 'server.properties$') server.properties file(s) in the zip (expected 0; the .example does not count)."
