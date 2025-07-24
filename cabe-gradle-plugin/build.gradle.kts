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
    
    // Skip publishing the Gradle Plugin publications to the staging directory
    tasks.findByName("publishPluginMavenPublicationToStagingDirectoryRepository")?.let { task ->
        task.enabled = false
        logger.lifecycle("Skipping pluginMaven publication to staging directory as per requirements")
    }
    
    tasks.findByName("publishCabePluginPluginMarkerMavenPublicationToStagingDirectoryRepository")?.let { task ->
        task.enabled = false
        logger.lifecycle("Skipping cabePluginPluginMarkerMaven publication to staging directory as per requirements")
    }
    
    // Disable all publication tasks to staging directory except for mavenJava
    tasks.withType<PublishToMavenRepository>().configureEach {
        if (name != "publishMavenJavaPublicationToStagingDirectoryRepository" && repository.name == "stagingDirectory") {
            enabled = false
            logger.lifecycle("Disabling task $name to prevent Gradle Plugin publication to staging directory")
        }
    }
    
    // Also disable the publishAllPublicationsToStagingDirectoryRepository task and recreate it to only include mavenJava
    tasks.findByName("publishAllPublicationsToStagingDirectoryRepository")?.let { task ->
        task.enabled = false
        logger.lifecycle("Disabling publishAllPublicationsToStagingDirectoryRepository task")
    }
    
    // Create a custom task that only publishes the mavenJava publication to staging directory
    tasks.register("publishOnlyMavenJavaToStagingDirectory") {
        dependsOn("publishMavenJavaPublicationToStagingDirectoryRepository")
        group = "publishing"
        description = "Publishes only the mavenJava publication to staging directory"
    }
    
    // Make the publishToStagingDirectory task depend on our custom task instead
    tasks.findByName("publishToStagingDirectory")?.let { task ->
        task.dependsOn.clear()
        task.dependsOn("publishOnlyMavenJavaToStagingDirectory")
        logger.lifecycle("Reconfigured publishToStagingDirectory task to only publish mavenJava publication")
    }
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
