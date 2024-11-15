#!/usr/bin/env bash

# This script first builds the plugin and then runs a test to verify assertions are present in the generated output 

FLAGS=--stacktrace

PROCESSOR_VERSION=$(./gradlew -q printProcessorVersion)
PLUGIN_VERSION=$(./gradlew -q printPluginVersion)

echo
echo "current versions"
echo "processor ....... " ${PROCESSOR_VERSION}
echo "gradle plugin ... " ${PLUGIN_VERSION}
echo

# update version information in documentation
cat -- > Writerside/v.list <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE vars SYSTEM "https://resources.jetbrains.com/writerside/1.0/vars.dtd">
<vars>
    <var name="product" value="Writerside"/>
    <var name="PROCESSOR_VERSION" value="${PROCESSOR_VERSION}"/>
    <var name="PLUGIN_VERSION" value="${PLUGIN_VERSION}"/>
</vars>
EOF

# run a second build and then a clean after successful execution to check files are not locked after executing gradle task
cd "`dirname $0`" \
&& ./gradlew clean build test publishToMavenLocal \
  && echo "build successful" \
&& ./gradlew --no-daemon -Dtest \
  cabe-gradle-plugin-test:clean \
  cabe-gradle-plugin-test:test-gradle-plugin:run \
  cabe-gradle-plugin-test:test-gradle-plugin-modular:run \
  ${FLAGS} \
  && ./gradlew --no-daemon -Dtest cabe-gradle-plugin-test:build ${FLAGS} \
  && ./gradlew --no-daemon -Dtest cabe-gradle-plugin-test:clean ${FLAGS} \
  && echo "plugin test successful" \
&& ./gradlew cabe-processor:shadowJar \
  && echo "shadow jar created" \
&& ./gradlew publishToMavenLocal \
  && echo "plugin published to local repository" \
&& ./gradlew --no-daemon -Dexamples \
  examples:clean \
  examples:hello:build \
  examples:hellofx:build \
  ${FLAGS} \
  && echo "compile examples successful" \
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
