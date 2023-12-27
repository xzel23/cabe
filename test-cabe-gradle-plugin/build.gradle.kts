buildscript {
    repositories {
        mavenLocal()
    }
    dependencies {
        classpath("com.dua3.cabe", "com.dua3.cabe.gradle.plugin", rootProject.extra["plugin_version"] as String)
        classpath("com.dua3.cabe", "cabe-annotations", rootProject.extra["annotations_version"] as String)
    }
}

plugins {
    id("application")
}

apply(plugin = "com.dua3.cabe")

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly(project(":cabe-annotations"))
    implementation("com.dua3.utility:utility:12.0.0-beta10")
}

application {
    mainClass = "com.dua3.cabe.test.coba.Coba"
}

tasks.withType<JavaExec> {
    enableAssertions = true
}
