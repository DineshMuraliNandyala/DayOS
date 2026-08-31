#!/bin/sh
#
# Copyright © 2015-2021 the original authors.
# Licensed under the Apache License, Version 2.0
#
# Gradle start up script for POSIX compatible shells
#

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

die() {
    echo
    echo "ERROR: $*"
    echo
    exit 1
}

warn() {
    echo "$*"
}

if [ -z "$JAVA_HOME" ]; then
    JAVACMD="java"
else
    JAVACMD="$JAVA_HOME/bin/java"
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
