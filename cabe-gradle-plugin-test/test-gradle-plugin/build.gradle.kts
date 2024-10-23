import com.dua3.cabe.gradle.CabeExtension
import com.dua3.cabe.processor.Config

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
    id("application")
}

apply(plugin = "com.dua3.cabe")

configure<CabeExtension> {
    config.set(Config.StandardConfig.STANDARD.config)
}

repositories {
    mavenLocal()
    mavenCentral()
}

application {
    mainClass = "com.dua3.cabe.test.coba.Coba"
}

tasks.withType<JavaExec> {
    enableAssertions = true
}
