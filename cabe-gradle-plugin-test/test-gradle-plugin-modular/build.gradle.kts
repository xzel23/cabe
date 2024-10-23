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

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // FIXME compileOnly(project(":cabe-annotations"))
}

application {
    mainClass = "com.dua3.cabe.test.modular.Modular"
}

tasks.withType<JavaExec> {
    enableAssertions = true
}
