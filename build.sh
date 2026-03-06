#!/usr/bin/env bash

# This script is now a wrapper around the Gradle fullBuild task.
# The complete build logic has been moved to build.gradle.kts.

cd "$(dirname "$0")" || exit 1
./gradlew fullBuild "$@"
