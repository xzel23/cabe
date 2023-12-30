pluginManagement {
    plugins {
        kotlin("jvm") version "1.9.21"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "cabe"

include("cabe-annotations", "cabe-processor", "cabe-gradle-plugin")

if (System.getProperty("notest") != null) {
    println("skipping plugin tests")
} else {
    include("cabe-gradle-plugin-test")
    include("cabe-gradle-plugin-test:test-gradle-plugin")
    include("cabe-gradle-plugin-test:test-gradle-plugin-modular")
}
