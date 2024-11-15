import com.dua3.cabe.gradle.CabeExtension
import com.dua3.cabe.processor.Configuration

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

plugins {
    id("java")
    id("application")
    id("com.dua3.cabe") version rootProject.extra["plugin_version"] as String
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.jspecify:jspecify:1.0.0")
}

application {
    mainClass.set("hello.Hello")
}
