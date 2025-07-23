import org.gradle.internal.extensions.stdlib.toDefaultLowerCase

plugins {
    id("java-library")
    id("application")
    id("maven-publish")
    id("signing")
    id("com.github.spotbugs") version "6.1.13"
    id("com.gradleup.shadow") version "8.3.8"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.jreleaser") version "1.19.0"
}

/////////////////////////////////////////////////////////////////////////////
object Meta {
    const val GROUP = "com.dua3.cabe"
    const val SCM = "https://github.com/xzel23/cabe.git"
    const val INCEPTION_YEAR = "2021"
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

fun isDevelopmentVersion(versionString: String): Boolean {
    val v = versionString.toDefaultLowerCase()
    val markers = listOf("snapshot", "alpha", "beta")
    return markers.any { marker -> v.contains("-$marker") || v.contains(".$marker") }
}

val isReleaseVersion = !isDevelopmentVersion(project.version.toString())
val isSnapshot = project.version.toString().toDefaultLowerCase().contains("snapshot")

file("src/main/java/com/dua3/cabe/processor/CabeProcessorMetaData.java")
    .writeText("""
        package com.dua3.cabe.processor;
        
        public class CabeProcessorMetaData {
            public static final String PROCESSOR_VERSION = "$version";
        }
    """)

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jspecify:jspecify:1.0.0")
    implementation("org.javassist:javassist:3.30.2-GA")

    testImplementation(platform("org.junit:junit-bom:5.13.3"))
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    runtimeOnly("org.apache.logging.log4j:log4j-core:2.25.1")
}

javafx {
    version = "17"
    modules = listOf("javafx.controls")
    configuration = "testImplementation"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withSourcesJar()
    withJavadocJar()
}

application {
    mainClass.set("com.dua3.cabe.processor.ClassPatcher")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.dua3.cabe.processor.ClassPatcher"
    }
}

tasks.shadowJar {
    archiveBaseName.set("${project.name}-all")
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.test {
    useJUnitPlatform()

    doFirst {
        val javaFxPath = configurations.named("testRuntimeClasspath")
            .get()
            .filter { it.name.contains("javafx") }
            .joinToString(File.pathSeparator)

        if (javaFxPath.isNotEmpty()) {
            systemProperty("org.openjfx.javafxplugin.path", javaFxPath)
        }
    }
}

// === JAVADOC / SOURCES for shaded jar ===
tasks.register<Javadoc>("javadocAll") {
    source = sourceSets["main"].allJava
    classpath = sourceSets["main"].runtimeClasspath
    setDestinationDir(file("${reporting.baseDirectory.asFile.get()}/javadocAll"))
}

tasks.register<Jar>("javadocAllJar") {
    archiveBaseName.set("${project.name}-all")
    archiveClassifier.set("javadoc")
    from(tasks.named("javadocAll"))
}

tasks.register<Copy>("sourcesAll") {
    from(sourceSets["main"].allSource)
    into(reporting.file("sourcesAll"))
}

tasks.register<Jar>("sourcesAllJar") {
    archiveBaseName.set("${project.name}-all")
    archiveClassifier.set("sources")
    from(tasks.named("sourcesAll"))
}

// === MAVEN PUBLISHING ===
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = Meta.GROUP
            artifactId = project.name
            version = project.version.toString()

            artifact(tasks.jar)
            artifact(tasks.named("javadocJar"))
            artifact(tasks.named("sourcesJar"))

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set(Meta.SCM)

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

            artifact(tasks.named("shadowJar"))
            artifact(tasks.named("javadocAllJar"))
            artifact(tasks.named("sourcesAllJar"))

            pom {
                name.set("${project.name}-all")
                description.set(project.description)
                url.set(Meta.SCM)

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
        // Sonatype snapshots for snapshot versions
        if (isSnapshot) {
            maven {
                name = "sonatypeSnapshots"
                url = uri("https://central.sonatype.com/repository/maven-snapshots/")
                credentials {
                    username = System.getenv("SONATYPE_USERNAME")
                    password = System.getenv("SONATYPE_PASSWORD")
                }
            }
        }

        // Always add root-level staging directory for JReleaser
        maven {
            name = "staging"
            url = rootProject.layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}

// === SIGNING ===
signing {
    isRequired = isReleaseVersion && gradle.taskGraph.hasTask("publish")
    sign(publishing.publications["maven"])
    sign(publishing.publications["mavenAll"])
}

// === STAGING TASK for JReleaser ===
tasks.register("publishToStagingRepository") {
    description = "Publishes Maven publications to the local staging directory"
    group = "publishing"
    
    dependsOn("publishMavenPublicationToStagingRepository")
    dependsOn("publishMavenAllPublicationToStagingRepository")
}

// Ensure jreleaserDeploy depends on staging publication
tasks.named("jreleaserDeploy") {
    dependsOn("publishToStagingRepository")
}

jreleaser {
    project {
        name.set("cabe-processor") // Use direct string instead of project.name to avoid circular reference
        version.set(project.version)
        
        group = Meta.GROUP
        authors.set(listOf(Meta.DEVELOPER_NAME))
        license.set(Meta.LICENSE_NAME)
        links {
            homepage.set(Meta.ORGANIZATION_URL)
        }
        inceptionYear.set(Meta.INCEPTION_YEAR)
        gitRootSearch.set(true)
    }
    
    release {
        github {
            // Configure how JReleaser handles snapshots
            draft.set(!isSnapshot)
            overwrite.set(true)
            skipTag.set(isSnapshot)
            milestone {
                close.set(!isSnapshot)
            }
        }
    }

    signing {
        // Only activate signing if environment variables are available
        val signingPublicKey = System.getenv("SIGNING_PUBLIC_KEY")
        val signingSecretKey = System.getenv("SIGNING_SECRET_KEY")
        val signingPassword = System.getenv("SIGNING_PASSWORD")
        
        if (signingPublicKey != null && signingSecretKey != null) {
            publicKey.set(signingPublicKey)
            secretKey.set(signingSecretKey)
            passphrase.set(signingPassword ?: "")
            active.set(org.jreleaser.model.Active.ALWAYS)
            armored.set(true)
        } else {
            // Skip signing if keys are not available
            active.set(org.jreleaser.model.Active.NEVER)
        }
    }

    deploy {
        maven {
            if (!isSnapshot) {
                println("adding release-deploy")
                mavenCentral {
                    create("release-deploy") {
                        active.set(org.jreleaser.model.Active.RELEASE)
                        url.set("https://central.sonatype.com/api/v1/publisher")
                        stagingRepositories.add("build/staging-deploy")
                        username.set(System.getenv("SONATYPE_USERNAME"))
                        password.set(System.getenv("SONATYPE_PASSWORD"))
                    }
                }
            } else {
                println("adding snapshot-deploy")
                nexus2 {
                    create("snapshot-deploy") {
                        active.set(org.jreleaser.model.Active.SNAPSHOT)
                        snapshotUrl.set("https://central.sonatype.com/repository/maven-snapshots/")
                        applyMavenCentralRules.set(true)
                        snapshotSupported.set(true)
                        closeRepository.set(true)
                        releaseRepository.set(true)
                        stagingRepositories.add("build/staging-deploy")
                        username.set(System.getenv("SONATYPE_USERNAME"))
                        password.set(System.getenv("SONATYPE_PASSWORD"))
                    }
                }
            }
        }
    }
}

// === SPOTBUGS ===
spotbugs {
    excludeFilter.set(rootProject.file("spotbugs-exclude.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports.create("html") {
        required.set(true)
        outputLocation = layout.buildDirectory.file("reports/spotbugs.html").get().asFile
        setStylesheet("fancy-hist.xsl")
    }
    reports.create("xml") {
        required.set(true)
        outputLocation = layout.buildDirectory.file("reports/spotbugs.xml").get().asFile
    }
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
