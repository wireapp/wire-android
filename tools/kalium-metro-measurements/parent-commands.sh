#!/bin/bash
# PARENT ONLY. This script stages fixtures, runs costly Gradle builds/tests, and cleans current injection.
set -euo pipefail
DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
: "${AFTER_ANDROID:?Set AFTER_ANDROID to the exact integrated Android commit}"
: "${AFTER_SDK:?Set AFTER_SDK to the exact integrated SDK commit}"
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
PREFIX="${RUN_LABEL:-20261020-v2-r1}"
WORK="$(python3 -c 'import sys;sys.path.insert(0,sys.argv[1]);from repo_paths import OUTPUT;print(OUTPUT)' "$DIR")"
python3 "$DIR/build-tools.py" --smoke
python3 "$DIR/parse-only.py"
# Baseline archives are created once and retained as ignored artifacts. No worktree/Git config changes.
if [ ! -f "$WORK/baseline-stage.json" ]; then
    python3 "$DIR/harness.py" stage-baseline --android-commit "$AFTER_ANDROID" --sdk-commit "$AFTER_SDK"
fi
python3 "$DIR/harness.py" stage-current --android-commit "$AFTER_ANDROID" --sdk-commit "$AFTER_SDK"
runs=()
for scenario in startup-only vm; do
    for pass in timing constructors; do
        for variant in baseline current; do
            label="$PREFIX-$variant-$scenario-$pass"
            python3 "$DIR/harness.py" run --variant "$variant" --scenario "$scenario" --pass "$pass" --label "$label"
            runs+=("$WORK/results/$label")
        done
    done
done
python3 "$DIR/summarize.py" "${runs[@]}"
python3 "$DIR/harness.py" cleanup --variant current
