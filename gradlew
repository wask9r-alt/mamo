#!/bin/sh
# Simplified gradlew for GitHub Actions
DIR="$(cd "$(dirname "$0")" && pwd)"
java -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
