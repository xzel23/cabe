package com.dua3.cabe.gradle;


import com.dua3.cabe.processor.ClassPatcher;
import com.dua3.cabe.processor.Configuration;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This tasks injects assertions for parameters marked as not allowing null values into the source code.
 */
public abstract class CabeTask extends DefaultTask {
    private final DirectoryProperty inputDirectory;
    private final DirectoryProperty outputDirectory;
    private final Provider<FileCollection> classPath;
    private final Provider<FileCollection> runtimeClassPath;
    private final Provider<String> javaExecutable;
    private final Property<Configuration> config;
    private final Property<Integer> verbosiy;

    /**
     * This task injects assertions for parameters marked as not allowing null values into the source code.
     *
     * @param objectFactory the Gradle object factory
     */
    @Inject
    public CabeTask(ObjectFactory objectFactory) {
        // Properties are created via Gradle's ObjectFactory
        inputDirectory = objectFactory.directoryProperty();
        outputDirectory = objectFactory.directoryProperty();
        classPath = objectFactory.property(FileCollection.class);
        runtimeClassPath = objectFactory.property(FileCollection.class);
        javaExecutable = objectFactory.property(String.class);
        config = objectFactory.property(Configuration.class);
        verbosiy = objectFactory.property(Integer.class);
    }

    /**
     * Retrieves the input directory for the Cabe task.
     *
     * @return The input directory for the Cabe task.
     */
    @InputFiles
    public DirectoryProperty getInputDirectory() {
        return inputDirectory;
    }

    /**
     * Retrieves the output directory for the Cabe task.
     *
     * @return The output directory for the Cabe task.
     */
    @OutputDirectory
    public DirectoryProperty getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Retrieves the class path for the Cabe task.
     *
     * @return The class path for the Cabe task.
     */
    @Classpath
    public Property<FileCollection> getClassPath() {
        return (Property<FileCollection>) classPath;
    }

    /**
     * Retrieves the class path for the Cabe task.
     *
     * @return The class path for the Cabe task.
     */
    @Classpath
    public Property<FileCollection> getRuntimeClassPath() {
        return (Property<FileCollection>) runtimeClassPath;
    }

    /**
     * Retrieves the path to the java command.
     *
     * @return The path to the java command.
     */
    @Input
    public Property<String> getJavaExecutable() {
        return (Property<String>) javaExecutable;
    }

    /**
     * Retrieves the configuration property for the Cabe task.
     *
     * @return The configuration property for the Cabe task.
     */
    @Input
    public Property<Configuration> getConfig() {
        return config;
    }

    /**
     * Retrieves the verbosity level property for the Cabe task.
     *
     * @return The verbosity level property for the Cabe task.
     */
    @Input
    public Property<Integer> getVerbosity() {
        return verbosiy;
    }

    @TaskAction
    void run() throws InterruptedException, GradleException {
        try {
            // Log input and output directories
            File inputDir = getInputDirectory().get().getAsFile();
            File outputDir = getOutputDirectory().get().getAsFile();
            getLogger().info("CabeTask executing with input directory: {}", inputDir);
            getLogger().info("CabeTask executing with output directory: {}", outputDir);
            
            // Check if input directory exists and has class files
            if (!inputDir.exists()) {
                getLogger().warn("Input directory does not exist: {}", inputDir);
            } else {
                File[] classFiles = inputDir.listFiles((dir, name) -> name.endsWith(".class"));
                getLogger().info("Input directory contains {} class files", classFiles != null ? classFiles.length : 0);
            }
            
            // Ensure output directory exists
            if (!outputDir.exists()) {
                getLogger().info("Creating output directory: {}", outputDir);
                outputDir.mkdirs();
            }
            
            String jarLocation = Paths.get(ClassPatcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
            getLogger().debug("ClassPatcher jar location: {}", jarLocation);
            
            String systemClassPath = System.getProperty("java.class.path");
            String classpath = Stream.concat(
                            getClassPath().get().getFiles().stream(),
                            getRuntimeClassPath().get().getFiles().stream()
                    )
                    .map(File::toString)
                    .distinct()
                    .collect(Collectors.joining(File.pathSeparator));

            String javaExec = javaExecutable.get();
            getLogger().debug("Java executable: {}", javaExec);

            int v = Objects.requireNonNullElse(verbosiy.get(), 0);
            String[] args = {
                    javaExec,
                    "-classpath", systemClassPath,
                    "-jar", jarLocation,
                    "-i", inputDir.toString(),
                    "-o", outputDir.toString(),
                    "-c", config.get().getConfigString(),
                    "-cp", classpath,
                    "-v", Integer.toString(v)
            };

            Logger logger = getLogger();
            // Always log the command being executed
            if (logger.isInfoEnabled()) {
                logger.info("{}", String.join(" ", args));
            }
            ProcessBuilder pb = new ProcessBuilder(args);

            Process process = pb.start();

            try (CopyOutput copyStdErr = new CopyOutput(process.errorReader(), s -> logger.debug("STDERR: {}", s));
                 CopyOutput ignore = new CopyOutput(process.inputReader(), s -> logger.debug("STDOUT: {}", s))) {
                int exitCode = process.waitFor();
                getLogger().info("Process completed with exit code: {}", exitCode);
                if (exitCode != 0) {
                    throw new GradleException("instrumenting class files failed\n\n" + copyStdErr);
                }
                
                // Check if output directory has class files
                File[] outputClassFiles = outputDir.listFiles((dir, name) -> name.endsWith(".class"));
                getLogger().debug("Output directory contains {} class files", outputClassFiles != null ? outputClassFiles.length : 0);
            }
        } catch (InterruptedException e) {
            getLogger().warn("instrumenting class files interrupted");
            throw e;
        } catch (Exception e) {
            throw new GradleException("An error occurred while instrumenting classes: " + e.getMessage(), e);
        }
    }

    /**
     * This class is responsible for copying the output of a Reader to a specified Consumer.
     * The first 10 lines are stored.
     */
    private class CopyOutput implements AutoCloseable {
        public static final int MAX_LINES = 10;
        Thread thread;
        List<String> firstLines = new ArrayList<>();

        CopyOutput(Reader reader, Consumer<String> printer) {
            thread = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(reader)) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        printer.accept(line);
                        if (firstLines.size() < MAX_LINES) {
                            firstLines.add(line);
                        } else if (firstLines.size() == MAX_LINES) {
                            firstLines.add("...");
                        }
                    }
                } catch (Exception e) {
                    getLogger().warn("exception reading ClassPatcher error output");
                }
            });
            thread.start();
        }

        @Override
        public void close() {
            try {
                thread.join(5000); // Wait 5000ms for the thread to die.
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (thread.isAlive()) {
                getLogger().warn("output thread did not stop");
                thread.interrupt();
            }
        }

        @Override
        public String toString() {
            return String.join("\n", firstLines);
        }
    }
}
