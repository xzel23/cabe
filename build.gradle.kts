import org.gradle.internal.extensions.stdlib.toDefaultLowerCase

plugins {
    id("java-library")
    alias(libs.plugins.jreleaser)
    alias(libs.plugins.task.tree)
    alias(libs.plugins.versions) apply false
}

/////////////////////////////////////////////////////////////////////////////
extra["GROUP"]              = "com.dua3.cabe"
extra["SCM"]                = "https://github.com/xzel23/cabe.git"
extra["INCEPTION_YEAR"]     = "2021"
extra["LICENSE_NAME"]       = "MIT License"
extra["LICENSE_URL"]        = "https://opensource.org/licenses/MIT"
extra["DEVELOPER_ID"]       = "axh"
extra["DEVELOPER_NAME"]     = "Axel Howind"
extra["DEVELOPER_EMAIL"]    = "axh@dua3.com"
extra["ORGANIZATION_NAME"]  = "dua3"
extra["ORGANIZATION_URL"]   = "https://www.dua3.com"
/////////////////////////////////////////////////////////////////////////////

val projectVersion = "4.3.1-SNAPSHOT"
version = projectVersion
extra["plugin_version"] = projectVersion
extra["processor_version"] = projectVersion

fun isDevelopmentVersion(versionString: String): Boolean {
    val v = versionString.toDefaultLowerCase()
    val markers = listOf("snapshot", "alpha", "beta", "rc")
    return markers.any { marker -> v.contains("-$marker") || v.contains(".$marker") }
}

val isReleaseVersion = project.hasProperty("release") || !isDevelopmentVersion(projectVersion)

// Make isReleaseVersion available as an extra property for subprojects
extra["isReleaseVersion"] = isReleaseVersion
val isSnapshot = project.version.toString().toDefaultLowerCase().contains("snapshot")

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "com.github.ben-manes.versions")

    dependencyLocking {
        lockAllConfigurations()
    }

    // Set version for all subprojects
    project.version = rootProject.version
    
    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withJavadocJar()
        withSourcesJar()

    }
}

tasks.register("printVersion") {
    group = "other"
    description = "Prints the project version"

    doLast {
        println(project.version)
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

                groupId = rootProject.extra["GROUP"].toString()
                artifactId = project.name
                version = project.version.toString()

                pom {
                    name.set(project.name)
                    description.set(project.description)
                    url.set(rootProject.extra["SCM"].toString())

                    licenses {
                        license {
                            name.set(rootProject.extra["LICENSE_NAME"].toString())
                            url.set(rootProject.extra["LICENSE_URL"].toString())
                        }
                    }

                    developers {
                        developer {
                            id.set(rootProject.extra["DEVELOPER_ID"].toString())
                            name.set(rootProject.extra["DEVELOPER_NAME"].toString())
                            email.set(rootProject.extra["DEVELOPER_EMAIL"].toString())
                            organization.set(rootProject.extra["ORGANIZATION_NAME"].toString())
                            organizationUrl.set(rootProject.extra["ORGANIZATION_URL"].toString())
                        }
                    }

                    scm {
                        connection.set("scm:git:${rootProject.extra["SCM"].toString()}")
                        developerConnection.set("scm:git:${rootProject.extra["SCM"].toString()}")
                        url.set(rootProject.extra["SCM"].toString())
                    }

                    withXml {
                        val root = asNode()
                        root.appendNode("inceptionYear", rootProject.extra["INCEPTION_YEAR"].toString())
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

// Aggregate all subprojects' publishToMavenLocal tasks into a root-level task
tasks.register("publishToMavenLocal") {
    group = "publishing"
    description = "Publish all subprojects' artifacts to local Maven repository"

    dependsOn(subprojects.mapNotNull { it.tasks.findByName("publishToMavenLocal") })
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

/////////////////////////////////////////////////////////////////////////////
// Full build lifecycle and regression tests (replaces build.sh)
/////////////////////////////////////////////////////////////////////////////

tasks.register("updateWritersideVersionList") {
    group = "documentation"
    val vListFile = file("Writerside/v.list")
    val processorVersion = rootProject.extra["processor_version"].toString()
    val pluginVersion = rootProject.extra["plugin_version"].toString()

    inputs.property("processorVersion", processorVersion)
    inputs.property("pluginVersion", pluginVersion)
    outputs.file(vListFile)

    doLast {
        vListFile.writeText("""
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE vars SYSTEM "https://resources.jetbrains.com/writerside/1.0/vars.dtd">
            <vars>
                <var name="product" value="Writerside"/>
                <var name="PROCESSOR_VERSION" value="$processorVersion"/>
                <var name="PLUGIN_VERSION" value="$pluginVersion"/>
            </vars>
        """.trimIndent() + "\n")
    }
}

// Helper function to register a gradlew task
fun Project.registerGradlewTask(name: String, vararg args: String, block: Exec.() -> Unit = {}) {
    tasks.register<Exec>(name) {
        group = "verification"
        workingDir = projectDir
        val gradlew = if (System.getProperty("os.name").lowercase().contains("windows")) "gradlew.bat" else "./gradlew"
        commandLine(gradlew, *args)
        block()
    }
}

registerGradlewTask("testPluginTasks", 
    "--no-daemon", "-Dtest",
    "cabe-gradle-plugin-test:clean",
    "cabe-gradle-plugin-test:test-gradle-plugin:run",
    "cabe-gradle-plugin-test:test-gradle-plugin-modular:run",
    "--stacktrace"
) {
    dependsOn("publishToMavenLocal")
}

registerGradlewTask("testPluginBuild", 
    "--no-daemon", "-Dtest", "cabe-gradle-plugin-test:build", "--stacktrace"
) {
    dependsOn("testPluginTasks")
}

registerGradlewTask("testPluginClean", 
    "--no-daemon", "-Dtest", "cabe-gradle-plugin-test:clean", "--stacktrace"
) {
    dependsOn("testPluginBuild")
}

registerGradlewTask("testExamplesFirstRun", 
    "--no-daemon", "-Dexamples",
    "examples:clean",
    "examples:hello:build",
    "examples:hellofx:build",
    "examples:cabe022:build",
    "examples:cabe022:app:build",
    "--stacktrace"
) {
    dependsOn("publishToMavenLocal")
}

registerGradlewTask("testExamplesConfigCacheClean", 
    "--no-daemon", "--configuration-cache", "-Dexamples", "examples:clean", "--stacktrace"
) {
    dependsOn("testExamplesFirstRun")
}

registerGradlewTask("testExamplesConfigCacheRun1", 
    "--no-daemon", "--configuration-cache", "-Dexamples",
    "examples:hello:build",
    "examples:hellofx:build",
    "examples:cabe022:build",
    "examples:cabe022:app:build",
    "--stacktrace"
) {
    dependsOn("testExamplesConfigCacheClean")
}

registerGradlewTask("testExamplesConfigCacheRun2", 
    "--no-daemon", "--configuration-cache", "-Dexamples",
    "examples:hello:build",
    "examples:hellofx:build",
    "examples:cabe022:build",
    "examples:cabe022:app:build",
    "--stacktrace"
) {
    dependsOn("testExamplesConfigCacheRun1")
}

tasks.register<Exec>("testMavenExample") {
    group = "verification"
    workingDir = file("examples/hello-maven")
    val pluginVersion = rootProject.extra["plugin_version"]
    val mvn = if (System.getProperty("os.name").lowercase().contains("windows")) "mvn.cmd" else "mvn"
    commandLine(mvn, "-Dcabe.version=$pluginVersion", "clean", "package")
    dependsOn("publishToMavenLocal")
}

// Helper task to run all tests in all subprojects
tasks.register("allTests") {
    group = "verification"
    description = "Runs all tests in all subprojects"
    subprojects.forEach { subproject ->
        dependsOn(subproject.tasks.withType<Test>())
    }
}

tasks.named("build") {
    dependsOn(
        "updateWritersideVersionList",
        "allTests",
        ":cabe-processor:shadowJar",
        "publishToMavenLocal",
        "publishToStagingDirectory",
        "testPluginClean", 
        "testExamplesConfigCacheRun2", 
        "testMavenExample",
        "aggregateJavadoc"
    )
    mustRunAfter("clean")
}

jreleaser {
    project {
        name.set(rootProject.name)
        version.set(projectVersion)
        group = extra["GROUP"].toString()
        authors.set(listOf(extra["DEVELOPER_NAME"].toString()))
        license.set(extra["LICENSE_NAME"].toString())
        links {
            homepage.set(extra["ORGANIZATION_URL"].toString())
        }
        inceptionYear.set(extra["INCEPTION_YEAR"].toString())
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
                        connectTimeout.set(300)
                        readTimeout.set(300)
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
                        connectTimeout.set(300)
                        readTimeout.set(300)
                    }
                }
            }
        }
    }
}
