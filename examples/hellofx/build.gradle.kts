import com.dua3.cabe.processor.Configuration

plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.dua3.cabe") version "3.0-beta-10"
}

repositories {
    mavenCentral()
}

javafx {
    version = "17"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("org.jspecify:jspecify:1.0.0")
}

cabe {
    config.set(Configuration.StandardConfig.DEVELOPMENT.config())
}

application {
    mainClass.set("hellofx.HelloFX")
}
