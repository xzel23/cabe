plugins {
    id("java")
    id("application")
    id("com.dua3.cabe") version "${projectVersion}"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jspecify:jspecify:1.0.0")
}

application {
    mainClass.set("hello.Hello")
}
