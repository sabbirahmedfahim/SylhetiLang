#!/bin/bash
set -u
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
./build.sh || exit 1
./sylhetic samples/test.syl || exit 1
./sylhetic samples/functions.syl || exit 1
./sylhetic samples/recursion.syl || exit 1
if ./sylhetic samples/err_test.syl; then exit 1; fi
if ./sylhetic samples/syntax_err.syl; then exit 1; fi
echo "All SylhetiLang checks passed."
