#!/usr/bin/env bash

# This script first builds the plugin and then runs a test to verify assertions are present in the generated output 

#FLAGS=--debug

# run a clean after successful execution to check files are not locked after executing gradle task
cd "`dirname $0`" \
&& ./gradlew -Dnotest clean build test publishToMavenLocal \
  && echo "build successful" \
&& ./gradlew --no-daemon test-cabe-gradle-plugin:clean test-cabe-gradle-plugin:run ${FLAGS} \
&& ./gradlew --no-daemon test-cabe-gradle-plugin:clean ${FLAGS} \
  && echo "non-modular test successful" \
&& ./gradlew --no-daemon test-cabe-gradle-plugin-with-modules:clean test-cabe-gradle-plugin-with-modules:run ${FLAGS} \
&& ./gradlew --no-daemon test-cabe-gradle-plugin-with-modules:clean ${FLAGS} \
  && echo "modular test successful" \
&& ./gradlew cabe-processor:shadowJar \
&& for D in regressiontest/* ; do \
  echo "test regressions: ${D}" ;\
  java -jar cabe-processor/build/libs/cabe-processor-all.jar -i "${D}/classes" -o "${D}/classes-processed" ; \
done \
&& ./gradlew publishToMavenLocal \
  && echo "plugin published to local repository" \
|| { echo "ERROR" ; exit 1 ; }

read -p "Publish plugin? " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
  echo "publishing gradle-processor JAR"
  ./gradlew cabe-processor:publish \
  && echo "publishing plugin" \
  && ./gradlew cabe-gradle-plugin:publishPlugins \
  && echo "plugin published" \
|| { echo "ERROR" ; exit 1 ; }
fi

echo "to publish the library, run ./gradlew publish"

echo "SUCCESS"
