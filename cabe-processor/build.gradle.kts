plugins {
    id("java-library")
    id("application")
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.shadow)
    alias(libs.plugins.javafxplugin)
}

project.version = rootProject.extra["processor_version"] as String
description = "The Cabe processor injects null checks based on JSpecify annotations into class files."

file("src/main/java/com/dua3/cabe/processor/CabeProcessorMetaData.java")
    .writeText("""
        package com.dua3.cabe.processor;
        
        /**
         * Meta data holder class.
         */
        public class CabeProcessorMetaData {
            private CabeProcessorMetaData() { /* utility class constructor */ }
            
            /**
             * The processor version string.
             */
            public static final String PROCESSOR_VERSION = "$version";
        }
    """)

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jspecify)
    implementation(libs.javassist)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.junit)
    runtimeOnly(libs.log4j.core)
}

javafx {
    version = libs.versions.javafx.get()
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
    archiveBaseName.set("${project.name}")
    archiveClassifier.set("all")
    mergeServiceFiles()
}

tasks.test {
    useJUnitPlatform()

    val testJavaVersion = project.findProperty("testJavaVersion")?.toString()
    if (testJavaVersion != null) {
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(testJavaVersion.toInt()))
        })
    }

    systemProperty("cabe.test.build.dir", layout.buildDirectory.dir("regression-test").get().asFile.absolutePath)
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

val testJdkVersions = listOf(17, 21, 25)
testJdkVersions.forEach { versionInt ->
    val taskName = "testJdk$versionInt"
    tasks.register<Test>(taskName) {
        group = "verification"
        description = "Runs ClassPatcherTest and RegressionTest with JDK $versionInt"

        val testSourceSet = sourceSets["test"]
        testClassesDirs = testSourceSet.output.classesDirs
        classpath = testSourceSet.runtimeClasspath

        useJUnitPlatform()

        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(versionInt))
        })

        filter {
            includeTestsMatching("com.dua3.cabe.processor.ClassPatcherTest")
            includeTestsMatching("com.dua3.cabe.processor.RegressionTest")
        }

        systemProperty("cabe.test.build.dir", layout.buildDirectory.dir("regression-$taskName").get().asFile.absolutePath)

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

    tasks.check {
        dependsOn(taskName)
    }
}

// === JAVADOC / SOURCES for shaded jar ===
tasks.register<Javadoc>("javadocAll") {
    group = "documentation"
    description = "Generates aggregated Javadoc for all subprojects"
    source = sourceSets["main"].allJava
    classpath = sourceSets["main"].runtimeClasspath
    setDestinationDir(reporting.baseDirectory.dir("javadocAll").get().asFile)
}

tasks.register<Jar>("javadocAllJar") {
    group = "documentation"
    description = "Assembles a jar archive containing the aggregated Javadoc for all subprojects"
    archiveBaseName.set("${project.name}")
    archiveClassifier.set("all-javadoc")
    from(tasks.named("javadocAll"))
}

tasks.register<Copy>("sourcesAll") {
    group = "documentation"
    description = "Copies all source files into a single directory"
    from(sourceSets["main"].allSource)
    into(reporting.baseDirectory.dir("sourcesAll"))
}

tasks.register<Jar>("sourcesAllJar") {
    group = "documentation"
    description = "Assembles a jar archive containing the aggregated sources for all subprojects"
    archiveBaseName.set("${project.name}")
    archiveClassifier.set("all-sources")
    from(tasks.named("sourcesAll"))
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

// Create a separate publication for the shadow JAR
configure<PublishingExtension> {
    publications {
        // Leave the mavenJava publication as is (created by the root build.gradle.kts)
        
        // Create a separate publication for the shadow JAR with artifactId "cabe-processor-all"
        create<MavenPublication>("shadowJar") {
            artifactId = "cabe-processor-all"
            groupId = "com.dua3.cabe"
            version = project.version.toString()
            
            // Add the shadow jar and its associated artifacts
            artifact(tasks.named("shadowJar")) {
                // Remove the classifier since we're using a different artifactId
                classifier = ""
            }
            artifact(tasks.named("javadocAllJar")) {
                classifier = "javadoc"
            }
            artifact(tasks.named("sourcesAllJar")) {
                classifier = "sources"
            }
            
            // Add complete POM information
            pom {
                name.set("${project.name}-all")
                description.set("${project.description ?: "CABE Processor with all dependencies"}")
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
                
                // Add inceptionYear for the shadowJar publication
                // This is needed because this publication is not affected by the root build.gradle.kts
                withXml {
                    val root = asNode()
                    root.appendNode("inceptionYear", rootProject.extra["INCEPTION_YEAR"].toString())
                }
            }
        }
    }
}
