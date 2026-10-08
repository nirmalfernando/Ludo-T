#!/bin/bash
# Phase 1 style headless local game (kept for continuity and the golden logs): ./run.sh <seed>
cd "$(dirname "$0")"
java -cp out/main/core com.ludot.Main "$@"
