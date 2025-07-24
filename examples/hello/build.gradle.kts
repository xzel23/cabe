import com.dua3.cabe.gradle.CabeExtension
import com.dua3.cabe.processor.Configuration

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath("com.dua3.cabe", "com.dua3.cabe.gradle.plugin", rootProject.extra["plugin_version"] as String)
    }
}

plugins {
    id("java")
    id("application")
}

apply(plugin = "com.dua3.cabe")

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
