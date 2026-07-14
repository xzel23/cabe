package com.dua3.cabe.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CabePluginFunctionalTest {

    @TempDir
    Path testProjectDir;

    @ParameterizedTest(name = "Compatible with Gradle {0}")
    @MethodSource("gradleVersions")
    void testPluginCompatibility(String gradleVersion) throws IOException {
        setupProject();

        GradleRunner runner = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("cabe", "--stacktrace", "--info")
                .withPluginClasspath();

        if (gradleVersion.equals("current")) {
            // use current version
        } else {
            runner.withGradleVersion(gradleVersion);
        }

        BuildResult result = runner.build();

        assertTrue(result.getOutput().contains("instrumenting classes using Cabe"));
        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
    }

    @ParameterizedTest(name = "Compatible with configuration cache in Gradle {0}")
    @MethodSource("gradleVersionsWithConfigurationCache")
    void testPluginCompatibilityWithConfigurationCache(String gradleVersion) throws IOException {
        setupProject();

        GradleRunner runner = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("cabe", "--stacktrace", "--info", "--configuration-cache")
                .withPluginClasspath();

        if (gradleVersion.equals("current")) {
            // use current version
        } else {
            runner.withGradleVersion(gradleVersion);
        }

        BuildResult result = runner.build();

        assertTrue(result.getOutput().contains("instrumenting classes using Cabe"));
        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
    }

    private static Stream<String> gradleVersions() {
        if (runCompatibilityMatrix()) {
            return Stream.of("8.6", "8.14", "9.0", "9.4.0", "current");
        }
        return Stream.of("current");
    }

    private static Stream<String> gradleVersionsWithConfigurationCache() {
        if (runCompatibilityMatrix()) {
            return Stream.of("8.14", "9.0", "9.4.0", "current");
        }
        return Stream.of("current");
    }

    private static boolean runCompatibilityMatrix() {
        return Boolean.getBoolean("cabe.gradle.compatibilityTest");
    }

    private void setupProject() throws IOException {
        // Create settings.gradle
        writeFile(testProjectDir.resolve("settings.gradle").toFile(), 
            "rootProject.name = 'test-project'");

        // Create build.gradle
        writeFile(testProjectDir.resolve("build.gradle").toFile(),
                """
                        plugins {
                            id 'java'
                            id 'com.dua3.cabe'
                        }
                        repositories { mavenCentral() }
                        dependencies { implementation 'org.jspecify:jspecify:1.0.0' }
                        """
        );

        // Create a dummy Java file
        Path javaDir = testProjectDir.resolve("src/main/java/com/example");
        Files.createDirectories(javaDir);
        writeFile(javaDir.resolve("Test.java").toFile(),
                """
                        package com.example;
                        import org.jspecify.annotations.*;
                        @NullMarked public class Test {
                            public void hello(String name) {}
                        }
                        """
        );
    }

    private void writeFile(File destination, String content) throws IOException {
        try (FileWriter writer = new FileWriter(destination)) {
            writer.write(content);
        }
    }
}
