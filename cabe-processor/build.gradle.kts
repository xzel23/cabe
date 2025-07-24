plugins {
    id("java-library")
    id("application")
    id("com.github.spotbugs") version "6.1.13"
    id("com.gradleup.shadow") version "8.3.8"
    id("org.openjfx.javafxplugin") version "0.1.0"
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
    archiveBaseName.set("${project.name}")
    archiveClassifier.set("all")
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
    archiveBaseName.set("${project.name}")
    archiveClassifier.set("all-javadoc")
    from(tasks.named("javadocAll"))
}

tasks.register<Copy>("sourcesAll") {
    from(sourceSets["main"].allSource)
    into(reporting.file("sourcesAll"))
}

tasks.register<Jar>("sourcesAllJar") {
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
