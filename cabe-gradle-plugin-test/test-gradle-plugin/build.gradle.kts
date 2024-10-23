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
    id("application")
}

apply(plugin = "com.dua3.cabe")

dependencies {
    implementation("org.jspecify:jspecify:1.0.0")
}

configure<CabeExtension> {
    config.set(Configuration.StandardConfig.STANDARD.config())
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
