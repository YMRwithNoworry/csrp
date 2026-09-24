#!/usr/bin/env bash
# Serialises Gradle invocations across the agents that share this checkout.
#
# Several agents build the same project at the same time; concurrent runs collide on
# build/classes and build/libs, producing misleading failures such as
# ":compileJava cleanup" / ":jar" file races or "Failed to clean up stale outputs".
# Wrapping Gradle in an exclusive lock removes that whole class of flakiness.
#
# Usage:
#   scripts/build-locked.sh build -x test --console=plain
#   scripts/build-locked.sh runGameTestServer --console=plain
#
# The lock lives in build/ (git-ignored). A lock older than LOCK_STALE_SECONDS is
# treated as abandoned and removed, so a killed agent cannot deadlock the others.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCK_DIR="$REPO_ROOT/build/.agent-gradle-lock"
LOCK_STALE_SECONDS=900
WAIT_ATTEMPTS=180
WAIT_SECONDS=5

mkdir -p "$REPO_ROOT/build"

acquired=0
for _ in $(seq 1 "$WAIT_ATTEMPTS"); do
    if mkdir "$LOCK_DIR" 2>/dev/null; then
        acquired=1
        break
    fi
    # Break a lock whose owner died.
    if [ -f "$LOCK_DIR/timestamp" ]; then
        lock_age=$(( $(date +%s) - $(cat "$LOCK_DIR/timestamp" 2>/dev/null || echo 0) ))
        if [ "$lock_age" -gt "$LOCK_STALE_SECONDS" ]; then
            echo "build-locked: removing stale lock (${lock_age}s old)" >&2
            rm -rf "$LOCK_DIR"
            continue
        fi
    fi
    sleep "$WAIT_SECONDS"
done

if [ "$acquired" -ne 1 ]; then
    echo "build-locked: could not acquire the Gradle lock after $((WAIT_ATTEMPTS * WAIT_SECONDS))s" >&2
    exit 1
fi

cleanup() {
    rm -rf "$LOCK_DIR"
}
trap cleanup EXIT INT TERM

date +%s > "$LOCK_DIR/timestamp"
echo "build-locked: lock acquired, running: gradlew $*"

export JAVA_HOME="${JAVA_HOME:-D:/MC/jdk/graalvm-community-25.3.4.1+1.1}"
cd "$REPO_ROOT" || exit 1
./gradlew.bat "$@"
status=$?
echo "build-locked: gradlew exited with $status"
exit $status
