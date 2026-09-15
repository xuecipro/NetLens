#!/usr/bin/env sh
# Minimal gradlew bootstrap: downloads Gradle if needed, then runs assemble.
# Prefer installing Gradle 8.7+ locally or using GitHub Actions.
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=java
fi

if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle not found. Install Gradle 8.7+ or use GitHub Actions CI."
  echo "https://gradle.org/install/"
  exit 1
fi

exec gradle "$@"
