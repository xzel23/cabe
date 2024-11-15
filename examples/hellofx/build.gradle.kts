import com.dua3.cabe.gradle.CabeExtension
import com.dua3.cabe.processor.Configuration

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.dua3.cabe") version rootProject.extra["plugin_version"] as String
}

repositories {
    mavenLocal()
    mavenCentral()
}

javafx {
    version = "17"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("org.jspecify:jspecify:1.0.0")
}
/*
cabe {
    config.set(Configuration.DEVELOPMENT)
}
*/
application {
    mainClass.set("hellofx.HelloFX")
}
