#!/usr/bin/env bash

# This script first builds the plugin and then runs a test to verify assertions are present in the generated output 

#FLAGS=--debug

# run a clean after successful execution to check files are not locked after executing gradle task
cd "`dirname $0`" \
&& ./gradlew -Dnotest clean build publishToMavenLocal \
  && echo "build successful" \
&& ./gradlew test-cabe-gradle-plugin:clean test-cabe-gradle-plugin:run ${FLAGS} \
  && ./gradlew test-cabe-gradle-plugin:clean \
  && echo "non-modular test successful" \
&& ./gradlew test-cabe-gradle-plugin-with-modules:clean test-cabe-gradle-plugin-with-modules:run ${FLAGS} \
  && ./gradlew  test-cabe-gradle-plugin-with-modules:clean \
  && echo "modular test successful" \
&& ./gradlew publishToMavenLocal \
  && echo "plugin published to local repository" \
|| { echo "ERROR" ; exit 1 ; }

read -p "Publish plugin? " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
  ./gradlew cabe-gradle-plugin:publishPlugins && echo "plugin published"
fi

echo "to publish the library, run ./gradlew publish"

echo "SUCCESS"
