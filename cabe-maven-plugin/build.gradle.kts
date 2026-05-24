plugins {
    id("java-library")
    id("maven-publish")
    alias(libs.plugins.versions)
    alias(libs.plugins.maven.plugin.development)
}

group = "com.dua3.cabe"
version = project.findProperty("plugin_version") as String? ?: project.version
description = "A plugin that adds assertions for annotated method parameters at compile time."

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(files("../cabe-processor/build/libs/cabe-processor-${rootProject.version}-all.jar") {
        builtBy(":cabe-processor:shadowJar")
    })

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
                val dependencies = if (dependenciesNode != null && dependenciesNode.isNotEmpty()) {
                    dependenciesNode[0] as groovy.util.Node
                } else {
                    asNode().appendNode("dependencies")
                }

                // Add cabe-processor-all dependency manually as it is included via files()
                val dep = dependencies.appendNode("dependency")
                dep.appendNode("groupId", "com.dua3.cabe")
                dep.appendNode("artifactId", "cabe-processor-all")
                dep.appendNode("version", project.version.toString())
                dep.appendNode("scope", "runtime")
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
