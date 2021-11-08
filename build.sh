#!/usr/bin/env bash

# This script first builds the plugin and then runs a test to verify assertions are present in the generated output 

cd `dirname $0`
./gradlew clean build && ./gradlew -Dtest test-cabe-gradle-plugin:clean test-cabe-gradle-plugin:run
