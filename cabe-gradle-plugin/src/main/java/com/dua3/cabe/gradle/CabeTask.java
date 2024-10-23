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
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This tasks injects assertions for parameters marked as not allowing null values into the source code.
 */
public abstract class CabeTask extends DefaultTask {
    private final DirectoryProperty inputDirectory;
    private final DirectoryProperty outputDirectory;
    private final Provider<FileCollection> classPath;
    private final Property<Configuration> config;

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
        config = objectFactory.property(Configuration.class);
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
     * Retrieves the configuration property for the Cabe task.
     *
     * @return The configuration property for the Cabe task.
     */
    @Input
    public Property<Configuration> getConfig() {
        return config;
    }

    @TaskAction
    void run() {
        try {
            String jarLocation = Paths.get(ClassPatcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
            String systemClassPath = System.getProperty("java.class.path");
            String projectClasspath = getClassPath().get().getFiles().stream()
                    .map(File::toString)
                    .collect(Collectors.joining(File.pathSeparator));

            String[] args = {
                    "java",
                    "-classpath", systemClassPath,
                    "-jar", jarLocation,
                    "-i", getInputDirectory().get().getAsFile().toString(),
                    "-o", getOutputDirectory().get().getAsFile().toString(),
                    "-c", config.get().getConfigString(),
                    "-cp", projectClasspath
            };

            Logger logger = getLogger();
            boolean verbose = logger.isDebugEnabled();

            if (verbose) {
                logger.debug("Instrumenting class files: " + String.join(" ", args));
            }

            logger.info(String.join(" ", args));
            ProcessBuilder pb = new ProcessBuilder(args);

            Process process = pb.start();

            try (CopyOutput copyStdErr = new CopyOutput(process.errorReader(), System.err::println);
                 CopyOutput copyStdOut = new CopyOutput(process.inputReader(), verbose ? System.out::println : s -> {})) {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new GradleException("instrumenting class files failed\n\n" + copyStdErr);
                }
            }
        } catch (Exception e) {
            throw new GradleException("An error occurred while instrumenting classes", e);
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
        public void close() throws Exception {
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
            return firstLines.stream().collect(Collectors.joining("\n"));
        }
    }
}
