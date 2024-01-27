import java.net.URI

plugins {
    id("java-library")
    id("application")
    id("maven-publish")
    id("signing")
    id("com.github.spotbugs") version "6.0.6"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

/////////////////////////////////////////////////////////////////////////////
object Meta {
    const val GROUP = "com.dua3.cabe"
    const val SCM = "https://github.com/xzel23/cabe.git"
    const val REPO = "public"
    const val LICENSE_NAME = "Apache License 2.0"
    const val LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0"
    const val DEVELOPER_ID = "axh"
    const val DEVELOPER_NAME = "Axel Howind"
    const val DEVELOPER_EMAIL = "axh@dua3.com"
    const val ORGANIZATION_NAME = "dua3"
    const val ORGANIZATION_URL = "https://www.dua3.com"
}
/////////////////////////////////////////////////////////////////////////////

project.version = rootProject.extra["processor_version"] as String
val isReleaseVersion = !project.version.toString().endsWith("SNAPSHOT")

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":cabe-annotations"))
    implementation("org.javassist:javassist:3.30.2-GA")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    runtimeOnly("org.apache.logging.log4j:log4j-core:2.22.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    application {
        mainClass.set("com.dua3.cabe.processor.ClassPatcher")
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.dua3.cabe.processor.ClassPatcher"
    }
}

tasks.shadowJar {
    archiveBaseName.set(project.name+"-all")
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.test {
    useJUnitPlatform()
}

tasks.register(/* name = */ "publishAllToMavenLocal") {
    dependsOn("publishMavenPublicationToMavenLocal", "publishMavenAllPublicationToMavenLocal")
}

// === publication: MAVEN = == >

// Create the publication with the pom configuration:
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = Meta.GROUP
            artifactId = project.name
            version = project.version.toString()

            artifact(tasks.jar)

            pom {
                withXml {
                    val root = asNode()
                    root.appendNode("description", project.description)
                    root.appendNode("name", project.name)
                    root.appendNode("url", Meta.SCM)
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
        create<MavenPublication>("mavenAll") {
            groupId = Meta.GROUP
            artifactId = "${project.name}-all"
            version = project.version.toString()

            artifact(tasks.shadowJar)

            pom {
                withXml {
                    val root = asNode()
                    root.appendNode("description", project.description)
                    root.appendNode("name", project.name + "-all")
                    root.appendNode("url", Meta.SCM)
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

    repositories {
        // Sonatype OSSRH
        maven {
            val releaseRepo = URI("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotRepo = URI("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (isReleaseVersion) releaseRepo else snapshotRepo
            credentials {
                username = project.properties["ossrhUsername"].toString()
                password = project.properties["ossrhPassword"].toString()
            }
        }
    }
}

// === sign artifacts
signing {
    isRequired = isReleaseVersion && gradle.taskGraph.hasTask("publish")
    sign(publishing.publications["maven"])
    sign(publishing.publications["mavenAll"])
}

// === SPOTBUGS ===
tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports.create("html") {
        required.set(true)
        outputLocation = project.layout.buildDirectory.file("reports/spotbugs.html").get().asFile
        setStylesheet("fancy-hist.xsl")
    }
    reports.create("xml") {
        required.set(true)
        outputLocation = project.layout.buildDirectory.file("reports/spotbugs.xml").get().asFile
    }
}

// === PUBLISHING ===
tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
