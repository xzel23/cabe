rootProject.name = "doc"

include("cabe-processor", "cabe-gradle-plugin")

dependencyResolutionManagement {

    versionCatalogs {
        create("libs") {
            plugin("foojay-resolver", "org.gradle.toolchains.foojay-resolver-convention").version("1.9.22")
            plugin("versions", "com.github.ben-manes.versions").version("0.51.0")
            plugin("test-logger", "com.adarshr.test-logger").version("4.0.0")
            plugin("spotbugs", "com.github.spotbugs").version("6.0.21")
            plugin("gradle-plugin-publish", "com.gradle.plugin-publish").version("1.2.1")
            plugin("task-tree", "com.dorongold.task-tree").version("2.1.1")

            version("dua3-utility", "13.0.1")
            version("jspecify", "1.0.0")

            library("dua3-utility", "com.dua3.utility", "utility").versionRef("dua3-utility")
            library("jspecify", "org.jspecify", "jspecify").versionRef("jspecify")
        }
    }
}

pluginManagement {
    plugins {
        kotlin("jvm") version "1.9.22"
    }
}

rootProject.name = "cabe"

if (System.getProperty("test") == null) {
    logger.info("skipping plugin tests")
} else {
    include("cabe-gradle-plugin-test")
    include("cabe-gradle-plugin-test:test-gradle-plugin")
    include("cabe-gradle-plugin-test:test-gradle-plugin-modular")
}
