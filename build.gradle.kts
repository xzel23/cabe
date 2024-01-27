plugins {
    id("java")
    id("com.dorongold.task-tree") version "2.1.1"
}

extra["plugin_version"] = "2.1-rc3"
extra["processor_version"] = "2.1-alpha"
extra["annotations_version"] = "2.0"

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withJavadocJar()
        withSourcesJar()
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
