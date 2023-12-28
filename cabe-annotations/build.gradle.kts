import java.net.URI

plugins {
    `java-library`
    `maven-publish`
    `signing`
}

description = "Java Annotations for Cabe"
version = project.findProperty("annotations_version") as String? ?: project.version

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
}

tasks.withType<Javadoc> {
    options.apply {
        encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

group = "com.dua3.cabe"

extra.apply {
    set("scm", "https://github.com/xzel23/cabe.git")
    set("developerId", "axh")
    set("developerName", "Axel Howind")
    set("developerEmail", "axh@dua3.com")
    set("organization", "dua3")
    set("organizationUrl", "https://www.dua3.com")
}

val isReleaseVersion = !version.toString().endsWith("SNAPSHOT")

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            from(components["java"])

            pom {
                withXml {
                    val root = asNode()
                    root.appendNode("description", project.description)
                    root.appendNode("name", project.name)
                    root.appendNode("url", project.extra["scm"])
                }

                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set(project.extra["developerId"] as String)
                        name.set(project.extra["developerName"] as String)
                        email.set(project.extra["developerEmail"] as String)
                        organization.set(project.extra["organization"] as String)
                        organizationUrl.set(project.extra["organizationUrl"] as String)
                    }
                }

                scm {
                    url.set(project.extra["scm"] as String)
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

signing {
    isRequired = isReleaseVersion && gradle.taskGraph.hasTask("publish")
    sign(publishing.publications["maven"])
}

tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.named("publishToMavenLocal"))
}
