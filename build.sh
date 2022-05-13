#!/usr/bin/env bash

# This script first builds the plugin and then runs a test to verify assertions are present in the generated output 

#FLAGS=--debug

cd `dirname $0` \
&& ./gradlew -Dnotest clean build publishToMavenLocal && echo "build successful" \
&& ./gradlew test-cabe-gradle-plugin:clean test-cabe-gradle-plugin:run ${FLAGS} && echo "non-modular test successful" \
&& ./gradlew test-cabe-gradle-plugin-with-modules:clean test-cabe-gradle-plugin-with-modules:run ${FLAGS} && echo "modular test successful" \
&& ./gradlew publishToMavenLocal && echo "plugin published to local repository" \
|| { echo "ERROR" ; exit 1 ; }

read -p "Publish library and plugin? " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
  ./gradlew cabe-gradle-plugin:publishPlugins publish && echo "plugin published"
fi 

echo "SUCCESS"
