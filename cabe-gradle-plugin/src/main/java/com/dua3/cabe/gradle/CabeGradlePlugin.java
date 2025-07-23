package com.dua3.cabe.gradle;

import com.dua3.cabe.processor.ClassPatcher;
import com.dua3.cabe.processor.Configuration;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Gradle plugin class for Cabe.
 */
public class CabeGradlePlugin implements Plugin<Project> {

    /**
     * Constructs a new instance of the CabeGradlePlugin.
     */
    public CabeGradlePlugin() {
        // nothing to do
    }

    @Override
    public void apply(Project project) {
        Logger log = project.getLogger();

        // check that JavaPlugin is loaded
        if (!project.getPlugins().hasPlugin(JavaPlugin.class)) {
            throw new IllegalStateException("the java plugin is required for cabe to work");
        }

        // create extension
        CabeExtension extension = project.getExtensions().create("cabe", CabeExtension.class, project);

        // Add the task before the evaluation of the project to allow access to values overloaded by the developer.
        project.afterEvaluate(p -> {
            log.info("plugin {} applied successfully", getClass().getSimpleName());

            // get the compileJava task
            JavaCompile compileJavaTask = Objects.requireNonNull(
                    project.getTasks().withType(JavaCompile.class).findByName("compileJava"),
                    "task 'compileJava' not found"
            );

            // make compilation task depend on instrumentation configuration
            compileJavaTask.getInputs().property("cabe.config", extension.getConfig().orElse(Configuration.STANDARD));
            compileJavaTask.getInputs().property("cabe.verbosity", extension.getVerbosity().orElse(0));

            // run istrumentation after compilation
            Logger logger = project.getLogger();
            Directory classesDir = compileJavaTask.getDestinationDirectory().get();
            Directory unprocessedClassesDir = project.getLayout().getBuildDirectory().dir("classes-cabe-input").get();
            Path javaExec = compileJavaTask.getJavaCompiler().get().getExecutablePath().getAsFile().toPath().getParent().resolve("java");

            org.gradle.api.artifacts.Configuration compileClasspath = project.getConfigurations().getByName("compileClasspath");
            org.gradle.api.artifacts.Configuration runtimeClasspath = project.getConfigurations().getByName("runtimeClasspath");
            String systemClassPath = System.getProperty("java.class.path");
            String classpath = Stream.concat(compileClasspath.getFiles().stream(), runtimeClasspath.getFiles().stream())
                    .map(File::toString)
                    .distinct()
                    .collect(Collectors.joining(File.pathSeparator));

            compileJavaTask.doLast(task -> instrumentClasses(
                    extension,
                    classesDir,
                    unprocessedClassesDir,
                    systemClassPath,
                    classpath,
                    javaExec,
                    logger)
            );
        });
    }

    private void instrumentClasses(
            CabeExtension extension,
            Directory classesDir,
            Directory unprocessedClassesDir,
            String systemClasspath,
            String classpath,
            Path javaExec,
            Logger logger
    ) throws GradleException {
        try {
            logger.info("instrumenting classes using Cabe\n  classesDir              : {}  unprocessedClassesDir:  {}",
                    classesDir.getAsFile().getAbsolutePath(),
                    unprocessedClassesDir.getAsFile().getAbsolutePath()
            );

            // move inputs
            copyFilesRecursively(classesDir.getAsFile().toPath(), unprocessedClassesDir.getAsFile().toPath(), logger);

            // process files
            String jarLocation = Paths.get(ClassPatcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();

            logger.debug("Java executable: {}", javaExec);

            int v = Objects.requireNonNullElse(extension.getVerbosity().get(), 0);
            String[] args = {
                    javaExec.toString(),
                    "-classpath", systemClasspath,
                    "-jar", jarLocation,
                    "-i", unprocessedClassesDir.toString(),
                    "-o", classesDir.toString(),
                    "-c", extension.getConfig().getOrElse(Configuration.STANDARD).getConfigString(),
                    "-cp", classpath,
                    "-v", Integer.toString(v)
            };

            // Always log the command being executed
            if (logger.isInfoEnabled()) {
                logger.info("{}", String.join(" ", args));
            }
            ProcessBuilder pb = new ProcessBuilder(args);

            Process process = pb.start();

            try (CopyOutput copyStdErr = new CopyOutput(process.errorReader(), logger::warn);
                 CopyOutput copyStdOut = new CopyOutput(process.inputReader(), logger::info)) {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new GradleException("instrumenting class files failed\n\n" + copyStdErr);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GradleException("Interrupted while instrumenting classes: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new GradleException("An error occurred while instrumenting classes: " + e.getMessage(), e);
        }
    }

    private static void copyFilesRecursively(Path source, Path target, Logger logger) throws IOException {
        try (Stream<Path> files = Files.walk(source)) {
            files.sorted()
                    .forEach(path -> {
                        try {
                            if (!Files.exists(path)) {
                                logger.warn("Cabe: source path does not exist: {}", path);
                            } else {
                                Path relative = source.relativize(path);
                                Path dest = target.resolve(relative);
                                logger.debug("copying:\n  path: {}\n  relative: {}\n  dest: {}", path, relative, dest);
                                if (Files.isDirectory(path)) {
                                    logger.debug("creating directory: {}", dest);
                                    Files.createDirectories(dest);
                                } else {
                                    logger.debug("copying file: {}", dest);
                                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                                }
                            }
                        } catch (IOException e) {
                            logger.warn("failed to copy file/directory: {}", path, e);
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }
}
