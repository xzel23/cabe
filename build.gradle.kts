plugins {
    id("java")
    id("com.dorongold.task-tree") version "4.0.1"
}

extra["plugin_version"] = "3.3.0-SNAPSHOT"
extra["processor_version"] = "3.3.0-SNAPSHOT"

subprojects {
    apply(plugin = "java")

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
                        || project.parent?.name == "examples") {
                        tasks.withType<AbstractPublishToMaven>()?.configureEach {
                            onlyIf { false }
                        }
                    }
                }
            }
        }
    }
}
