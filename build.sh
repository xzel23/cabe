#!/usr/bin/env bash

# This script first builds the plugin and then runs a test to verify assertions are present in the generated output 

FLAGS=--stacktrace

PROCESSOR_VERSION=$(./gradlew -Dnotest -q printProcessorVersion)
PLUGIN_VERSION=$(./gradlew -Dnotest -q printPluginVersion)
ANNOTATIONS_VERSION=$(./gradlew -Dnotest -q printAnnotationsVersion)

echo
echo "current versions"
echo "annotations ..... " ${ANNOTATIONS_VERSION}
echo "processor ....... " ${PROCESSOR_VERSION}
echo "gradle plugin ... " ${PLUGIN_VERSION}
echo

# run a second build and then a clean after successful execution to check files are not locked after executing gradle task
cd "`dirname $0`" \
&& ./gradlew -Dnotest clean build test publishToMavenLocal \
  && echo "build successful" \
&& ./gradlew --no-daemon \
  cabe-gradle-plugin-test:clean \
  cabe-gradle-plugin-test:test-gradle-plugin:run \
  cabe-gradle-plugin-test:test-gradle-plugin-modular:run \
  ${FLAGS} \
  && ./gradlew --no-daemon cabe-gradle-plugin-test:build ${FLAGS} \
  && ./gradlew --no-daemon cabe-gradle-plugin-test:clean ${FLAGS} \
  && echo "plugin test successful" \
&& ./gradlew cabe-processor:shadowJar \
  && echo "shadow jar created" \
&& for D in regressiontest/* ; do \
    echo "test regressions: ${D}" ;\
    java -jar cabe-processor/build/libs/cabe-processor-all-${PROCESSOR_VERSION}.jar -i "${D}/classes" -o "${D}/classes-processed" ; \
  done \
&& ./gradlew publishToMavenLocal \
  && echo "plugin published to local repository" \
|| { echo "ERROR" ; exit 1 ; }

read -p "publish cabe-processor? " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
  echo "publishing cabe-processor jar"
  ./gradlew cabe-processor:publish --no-parallel \
  && echo "cabe-processor jar published" \
  || { echo "ERROR" ; exit 1 ; }
fi

read -p "publish cabe-plugin? " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
  echo "publishing cabe-plugin" \
  && ./gradlew cabe-gradle-plugin:publishPlugins --no-parallel \
  && echo "cabe-plugin published" \
  || { echo "ERROR" ; exit 1 ; }
fi

echo "SUCCESS"
