#!/usr/bin/env sh
APP_BASE_DIR=$(dirname "$0")
CLASSPATH="$APP_BASE_DIR/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$CLASSPATH" ]; then
  echo "gradle-wrapper.jar is missing. If you have Gradle installed, run: gradle wrapper --gradle-version 9.0.0"
  echo "Alternatively, install Gradle or add the missing jar."
  exit 1
fi
exec "$APP_BASE_DIR"/gradlew "$@"
