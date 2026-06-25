plugins {
    id("java-library")
    id("maven-publish")
    alias(libs.plugins.maven.plugin.development)
}

group = "com.dua3.cabe"
version = rootProject.extra["plugin_version"] as String
description = "A plugin that adds assertions for annotated method parameters at compile time."

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(path = ":cabe-processor", configuration = "shadow"))

    compileOnlyApi(libs.maven.plugin.api)
    compileOnlyApi(libs.maven.plugin.annotations)
    compileOnlyApi(libs.maven.core)
}

// Disable Gradle Module Metadata to ensure the modified POM is used
tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            pom.withXml {
                val dependenciesNode = asNode().get("dependencies") as? groovy.util.NodeList
                if (dependenciesNode != null && dependenciesNode.isNotEmpty()) {
                    val dependencies = dependenciesNode[0] as groovy.util.Node
                    dependencies.children().forEach {
                        val dep = it as groovy.util.Node
                        val artifactIdNode = dep.get("artifactId") as? groovy.util.NodeList
                        if (artifactIdNode != null && artifactIdNode.isNotEmpty()) {
                            val artifactId = artifactIdNode[0] as groovy.util.Node
                            if (artifactId.text() == "cabe-processor") {
                                artifactId.setValue("cabe-processor-all")
                            }
                        }
                    }
                }
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
