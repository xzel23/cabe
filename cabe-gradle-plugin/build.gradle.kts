plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
    id("com.github.ben-manes.versions") version "0.50.0"
}

description = "The Gradle plugin adds null checks based on JSpecify annotations at compile time."
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

tasks.named("publishMavenJavaPublicationToMavenLocal") {
    dependsOn("signPluginMavenPublication")
}

tasks.named("publishMavenJavaPublicationToStagingDirectoryRepository") {
    dependsOn("signPluginMavenPublication")
}

// Use afterEvaluate to ensure all tasks are created before configuration
afterEvaluate {
    tasks.findByName("publishPluginMavenPublicationToMavenLocal")?.dependsOn("signMavenJavaPublication")
    tasks.findByName("publishPluginMavenPublicationToStagingDirectoryRepository")?.dependsOn("signMavenJavaPublication")
}

// Access isReleaseVersion from root project
val isReleaseVersion = rootProject.extra["isReleaseVersion"] as Boolean

afterEvaluate {
    // Configure publishPlugins task to be skipped when not a release version
    tasks.named("publishPlugins") {
        onlyIf {
            if (isReleaseVersion) {
                logger.lifecycle("Publishing plugin to Gradle Plugin Portal (release version)")
                true
            } else {
                logger.lifecycle("Skipping plugin publishing to Gradle Plugin Portal (non-release version)")
                false
            }
        }
    }
}

gradlePlugin {
    website = rootProject.extra["SCM"] as String
    vcsUrl = rootProject.extra["SCM"] as String

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

// The metadata is already added by the root build.gradle.kts
// No need to add it here to avoid duplication