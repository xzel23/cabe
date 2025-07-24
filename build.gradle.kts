import org.gradle.internal.extensions.stdlib.toDefaultLowerCase

plugins {
    id("java-library")
    id("org.jreleaser") version "1.19.0"
    id("com.dorongold.task-tree") version "4.0.1"
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

val projectVersion = "3.3.0"
version = projectVersion
extra["plugin_version"] = projectVersion
extra["processor_version"] = projectVersion

fun isDevelopmentVersion(versionString: String): Boolean {
    val v = versionString.toDefaultLowerCase()
    val markers = listOf("snapshot", "alpha", "beta", "rc")
    return markers.any { marker -> v.contains("-$marker") || v.contains(".$marker") }
}

val isReleaseVersion = !isDevelopmentVersion(projectVersion)

// Make isReleaseVersion available as an extra property for subprojects
extra["isReleaseVersion"] = isReleaseVersion
val isSnapshot = project.version.toString().toDefaultLowerCase().contains("snapshot")

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    
    // Set version for all subprojects
    project.version = rootProject.version
    
    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withJavadocJar()
        withSourcesJar()

        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}

tasks.register("printProcessorVersion") {
    group = "other"
    description = "Prints the processor version"

    doLast {
        println(rootProject.extra["processor_version"])
    }
}

tasks.register("printPluginVersion") {
    group = "other"
    description = "Prints the plugin version"

    doLast {
        println(rootProject.extra["plugin_version"])
    }
}

allprojects {
    pluginManager.withPlugin("maven-publish") {
        extensions.configure<PublishingExtension> {
            publications {
                all {
                    // Exclude the examples and its subprojects from being published
                    if (project.name == "cabe-plugin-test"
                        || project.name == "examples"
                        || project.parent?.name == "examples"
                    ) {
                        tasks.withType<AbstractPublishToMaven>().configureEach {
                            onlyIf { false }
                        }
                    }
                }
            }
        }
    }
}

subprojects {
    configure<PublishingExtension> {
        // Repositories for publishing
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
                name = "stagingDirectory"
                url = rootProject.layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])

                groupId = Meta.GROUP
                artifactId = project.name
                version = project.version.toString()

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
                        connection.set("scm:git:${Meta.SCM}")
                        developerConnection.set("scm:git:${Meta.SCM}")
                        url.set(Meta.SCM)
                    }

                    withXml {
                        val root = asNode()
                        root.appendNode("inceptionYear", "2019")
                    }
                }
            }
        }
    }

    // Task to publish to staging directory per subproject
    val publishToStagingDirectory by tasks.registering {
        group = "publishing"
        description = "Publish artifacts to root staging directory for JReleaser"

        dependsOn(tasks.withType<PublishToMavenRepository>().matching {
            it.repository.name == "stagingDirectory"
        })
    }

    // Signing configuration deferred until after evaluation
    afterEvaluate {
        configure<SigningExtension> {
            val shouldSign = !project.version.toString().lowercase().contains("snapshot")
            isRequired = shouldSign && gradle.taskGraph.hasTask("publish")

            val publishing = project.extensions.getByType<PublishingExtension>()

            if (project.name.endsWith("-bom")) {
                if (publishing.publications.names.contains("bomPublication")) {
                    sign(publishing.publications["bomPublication"])
                }
            } else {
                if (publishing.publications.names.contains("mavenJava")) {
                    sign(publishing.publications["mavenJava"])
                }
            }
        }
    }

    // set the project description after evaluation because it is not yet visible when the POM is first created
    afterEvaluate {
        project.extensions.configure<PublishingExtension> {
            publications.withType<MavenPublication> {
                pom {
                    if (description.orNull.isNullOrBlank()) {
                        description.set(project.description ?: "No description provided")
                    }
                }
            }
        }
    }
}

/////////////////////////////////////////////////////////////////////////////
// Root project tasks and JReleaser configuration
/////////////////////////////////////////////////////////////////////////////

// Aggregate all subprojects' publishToStagingDirectory tasks into a root-level task
tasks.register("publishToStagingDirectory") {
    group = "publishing"
    description = "Publish all subprojects' artifacts to root staging directory for JReleaser"

    dependsOn(subprojects.mapNotNull { it.tasks.findByName("publishToStagingDirectory") })
}

// add a task to create aggregate javadoc in the root projects build/docs/javadoc folder
tasks.register<Javadoc>("aggregateJavadoc") {
    group = "documentation"
    description = "Generates aggregated Javadoc for all subprojects"

    setDestinationDir(layout.buildDirectory.dir("docs/javadoc").get().asFile)
    title = "${rootProject.name} ${project.version} API"

    // Disable module path inference
    modularity.inferModulePath.set(false)

    // Configure the task to depend on all subprojects' javadoc tasks
    val filteredProjects = subprojects.filter {
        !it.name.endsWith("-bom") && !it.name.contains("samples")
    }

    dependsOn(filteredProjects.map { it.tasks.named("javadoc") })

    // Collect all Java source directories from subprojects, excluding module-info.java files
    source(filteredProjects.flatMap { project ->
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val main = sourceSets.findByName("main")
        main?.allJava?.filter { file ->
            !file.name.equals("module-info.java")
        } ?: files()
    })

    // Collect all classpaths from subprojects
    classpath = files(filteredProjects.flatMap { project ->
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val main = sourceSets.findByName("main")
        main?.compileClasspath ?: files()
    })

    // Add runtime classpath to ensure all dependencies are available
    classpath += files(filteredProjects.flatMap { project ->
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val main = sourceSets.findByName("main")
        main?.runtimeClasspath ?: files()
    })

    // Apply the same Javadoc options as in subprojects
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        addStringOption("Xdoclint:all,-missing/private")
        links("https://docs.oracle.com/en/java/javase/21/docs/api/")
        use(true)
        noTimestamp(true)
        windowTitle = "${rootProject.name} ${project.version} API"
        docTitle = "${rootProject.name} ${project.version} API"
        header = "${rootProject.name} ${project.version} API"
        // Set locale to English to ensure consistent language in generated documentation
        locale = "en_US"
        // Disable module path to avoid module-related errors
        addBooleanOption("module-path", false)
    }
}

tasks.named("jreleaserDeploy") {
    dependsOn("publishToStagingDirectory", "aggregateJavadoc")
}

tasks.named("jreleaserUpload") {
    dependsOn("publishToStagingDirectory", "aggregateJavadoc")
}

jreleaser {
    project {
        name.set(rootProject.name)
        version.set(projectVersion)
        group = Meta.GROUP
        authors.set(listOf(Meta.DEVELOPER_NAME))
        license.set(Meta.LICENSE_NAME)
        links {
            homepage.set(Meta.ORGANIZATION_URL)
        }
        inceptionYear.set(Meta.INCEPTION_YEAR)
        gitRootSearch.set(true)
    }

    signing {
        publicKey.set(System.getenv("SIGNING_PUBLIC_KEY"))
        secretKey.set(System.getenv("SIGNING_SECRET_KEY"))
        passphrase.set(System.getenv("SIGNING_PASSWORD"))
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
    }

    deploy {
        maven {
            if (!isSnapshot) {
                logger.info("adding release-deploy")
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
                logger.info("adding snapshot-deploy")
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
