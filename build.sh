#!/bin/bash
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$ROOT/build/classes"
find "$ROOT/src" -name '*.java' > "$ROOT/build/sources.txt"
javac --release 21 -encoding UTF-8 -d "$ROOT/build/classes" @"$ROOT/build/sources.txt"
echo "Build successful."
