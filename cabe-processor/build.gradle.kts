import java.net.URI

plugins {
    id("java")
    id("maven-publish")
    id("signing")
    id("com.github.spotbugs") version "6.0.4"
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

project.version = rootProject.extra["annotations_version"] as String
val isReleaseVersion = !project.version.toString().endsWith("SNAPSHOT")

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":cabe-annotations"))
    implementation("org.javassist:javassist:3.30.0-GA")
    implementation("com.dua3.utility:utility:12.0.0-beta10")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

// === publication: MAVEN = == >

// Create the publication with the pom configuration:
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = Meta.GROUP
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

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
}

// === SPOTBUGS ===
// spotbugs.excludeFilter.set(rootProject.file("spotbugs-exclude.xml"))

configurations.named("spotbugs").configure {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.ow2.asm") {
            useVersion("9.5")
            because("Asm 9.5 is required for JDK 21 support")
        }
    }
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>() {
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
tasks.withType<PublishToMavenRepository>() {
    dependsOn(tasks.publishToMavenLocal)
}

tasks.withType<Jar>() {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
