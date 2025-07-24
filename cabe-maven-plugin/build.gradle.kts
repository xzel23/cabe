import groovy.util.Node
import groovy.util.NodeList

plugins {
    id("java-library")
    id("maven-publish")
    id("com.github.ben-manes.versions") version "0.50.0"
    id("de.benediktritter.maven-plugin-development") version "0.4.3"
}

group = "com.dua3.cabe"
version = project.findProperty("plugin_version") as String? ?: project.version
description = "A plugin that adds assertions for annotated method parameters at compile time."

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    var processor_version = rootProject.extra["processor_version"] as String
    implementation("com.dua3.cabe:cabe-processor-all:${processor_version}")

    compileOnlyApi("org.apache.maven:maven-plugin-api:3.9.9")
    compileOnlyApi("org.apache.maven.plugin-tools:maven-plugin-annotations:3.15.1")
    compileOnlyApi("org.apache.maven:maven-core:3.9.9")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
