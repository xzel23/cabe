plugins {
    id("java")
    id("com.dorongold.task-tree") version "2.1.1"
}

extra["plugin_version"] = "3.0-beta-4"
extra["processor_version"] = "3.0-beta-4"

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withJavadocJar()
        withSourcesJar()
    }
}

tasks.create("printProcessorVersion") {
    doLast {
        println(rootProject.extra["processor_version"])
    }
}

tasks.create("printPluginVersion") {
    doLast {
        println(rootProject.extra["plugin_version"])
    }
}

allprojects {
    tasks.create("printTaskInputsAndOutputs") {
        doLast {
            project.tasks.forEach {
                println("--------------------------------------------------------------------------------")
                println(" Task '${project.name}:${it.name}'")
                println("--------------------------------------------------------------------------------")
                println("")

                println("File inputs:")
                it.inputs.files.forEach {
                    println(" - ${it}")
                }
                println("")

                println("Property inputs:")
                it.inputs.properties.forEach {
                    println(" - ${it}")
                }
                println("")

                println("File outputs:")
                it.outputs.files.forEach {
                    println(" - ${it}")
                }
                println("")

                println("--------------------------------------------------------------------------------")
                println("")
            }
        }
    }
}
