import groovy.util.Node
import groovy.util.NodeList

plugins {
    id("java-library")
    id("maven-publish")
    id("com.github.ben-manes.versions") version "0.50.0"
    id("de.benediktritter.maven-plugin-development") version "0.4.3"
}

group = "com.dua3.cabe"
version = project.findProperty("plugin_version") as String? ?: project.version
description = "A plugin that adds assertions for annotated method parameters at compile time."

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    var processor_version = rootProject.extra["processor_version"] as String
    implementation("com.dua3.cabe:cabe-processor-all:${processor_version}")

    compileOnlyApi("org.apache.maven:maven-plugin-api:3.9.9")
    compileOnlyApi("org.apache.maven.plugin-tools:maven-plugin-annotations:3.15.1")
    compileOnlyApi("org.apache.maven:maven-core:3.9.9")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

/////////////////////////////////////////////////////////////////////////////
object Meta {
    const val SCM = "https://github.com/xzel23/cabe.git"
    const val LICENSE_NAME = "Apache License 2.0"
    const val LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0"
    const val DEVELOPER_ID = "axh"
    const val DEVELOPER_NAME = "Axel Howind"
    const val DEVELOPER_EMAIL = "axh@dua3.com"
    const val ORGANIZATION_NAME = "dua3"
    const val ORGANIZATION_URL = "https://www.dua3.com"
}
/////////////////////////////////////////////////////////////////////////////

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group as String?
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

            pom {
                description.set(project.description)
                packaging = "maven-plugin"

                withXml {
                    val root = asNode()
                    val dependenciesNode = (root.get("dependencies") as NodeList)[0] as Node

                    // set maven dependencies with provided scope
                    dependenciesNode.children().forEach { dep ->
                        val dependencyNode = dep as Node
                        val artifactId =
                            ((dependencyNode.get("artifactId") as NodeList)[0] as Node).text()
                        if (artifactId == "maven-plugin-api"
                            || artifactId == "maven-plugin-annotations"
                            || artifactId == "maven-core"
                        ) {
                            val scopeNodes = (dependencyNode.get("scope") as NodeList)
                            if (scopeNodes.isEmpty()) {
                                dependencyNode.appendNode("scope", "provided")
                            } else {
                                (scopeNodes[0] as Node).setValue("provided")
                            }
                        }
                    }
                }

                licenses {
                    license {
                        name.set(Meta.LICENSE_NAME)
                        url.set(Meta.LICENSE_URL)
                    }
                }
                developers {
                    developer {
                        id.set(Meta.DEVELOPER_ID)
                        name.set(Meta.DEVELOPER_NAME)
                        email.set(Meta.DEVELOPER_EMAIL)
                        organization.set(Meta.ORGANIZATION_NAME)
                        organizationUrl.set(Meta.ORGANIZATION_URL)
                    }
                }

                scm {
                    url.set(Meta.SCM)
                }
            }
        }
    }
}

