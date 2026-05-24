import com.dua3.cabe.gradle.CabeExtension
import com.dua3.cabe.processor.Configuration

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath("com.dua3.cabe", "com.dua3.cabe.gradle.plugin", rootProject.extra["plugin_version"] as String)
    }
}

plugins {
    id("java")
    id("application")
    alias(libs.plugins.javafxplugin)
}

apply(plugin = "com.dua3.cabe")

repositories {
    mavenLocal()
    mavenCentral()
}

javafx {
    version = libs.versions.javafx.get()
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation(libs.jspecify)
}
/*
cabe {
    config.set(Configuration.DEVELOPMENT)
}
*/
application {
    mainClass.set("hellofx.HelloFX")
}
