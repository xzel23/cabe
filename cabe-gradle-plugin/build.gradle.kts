plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
    id("com.github.ben-manes.versions") version "0.50.0"
}

version = project.findProperty("plugin_version") as String? ?: project.version

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.jspecify:jspecify:1.0.0")
    var processor_version = rootProject.extra["processor_version"] as String
    implementation("com.dua3.cabe:cabe-processor-all:${processor_version}")
}

tasks.named("compileJava") {
    dependsOn(":cabe-processor:publishToMavenLocal")
}

gradlePlugin {
    website = "https://github.com/xzel23/cabe"
    vcsUrl = "https://github.com/xzel23/cabe"

    plugins {
        create("cabePlugin") {
            id = "com.dua3.cabe"
            group = "com.dua3"
            displayName = "Plugin for adding assertions during compile time"
            description = "A plugin that adds assertions for annotated method parameters at compile time."
            tags = listOf("java", "NonNull", "Nullable", "null check", "assertion")
            implementationClass = "com.dua3.cabe.gradle.CabeGradlePlugin"
        }
    }
}