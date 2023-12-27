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
    include("test-cabe-gradle-plugin")
    include ("test-cabe-gradle-plugin-with-modules")
}
