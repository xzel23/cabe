rootProject.name = "cabe"

include(
    "cabe-processor",
    "cabe-gradle-plugin",
    "cabe-maven-plugin"
)


pluginManagement {
    plugins {
        kotlin("jvm") version "1.9.22"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

if (System.getProperty("test") == null) {
    logger.info("skipping plugin tests")
} else {
    include(
        "cabe-gradle-plugin-test",
        "cabe-gradle-plugin-test:test-gradle-plugin",
        "cabe-gradle-plugin-test:test-gradle-plugin-modular"
    )
}

if (System.getProperty("examples") == null) {
    logger.info("skipping examples")
} else {
    include(
        "examples",
        "examples:hello",
        "examples:hellofx",
        "examples:cabe022",
        "examples:cabe022:app"
    )
}
