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
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
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
@CacheableTask
public abstract class CabeTask extends DefaultTask {
    private final DirectoryProperty originalClassesDir;
    private final DirectoryProperty classesDir;
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
        originalClassesDir = objectFactory.directoryProperty();
        classesDir = objectFactory.directoryProperty();
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
    @PathSensitive(PathSensitivity.RELATIVE)
    public DirectoryProperty getOriginalClassesDir() {
        return originalClassesDir;
    }

    /**
     * Retrieves the output directory for the Cabe task.
     *
     * @return The output directory for the Cabe task.
     */
    @OutputDirectory
    public DirectoryProperty getClassesDir() {
        return classesDir;
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
        Logger logger = getLogger();

        try {
            // move inputs
            File cabeOutputDir = getClassesDir().get().getAsFile();
            if (!cabeOutputDir.isDirectory()) {
                logger.info("classes directory does not exist: {}", cabeOutputDir.getAbsolutePath());
                return;
            }
            
            File cabeInputDir = getOriginalClassesDir().get().getAsFile();
            if (!(cabeInputDir.exists() && cabeInputDir.isDirectory()) && !cabeInputDir.mkdirs()) {
                throw new GradleException("could not create intermediate directory " + cabeInputDir.getAbsolutePath());
            }

            copyClassesToCabeInputDir(cabeOutputDir, cabeInputDir);

            // process files
            String jarLocation = Paths.get(ClassPatcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
            String systemClassPath = System.getProperty("java.class.path");
            String classpath = Stream.concat(
                            getClassPath().get().getFiles().stream(),
                            getRuntimeClassPath().get().getFiles().stream()
                    )
                    .map(File::toString)
                    .distinct()
                    .collect(Collectors.joining(File.pathSeparator));

            String javaExec = javaExecutable.get();
            logger.info("Java executable: {}", javaExec);

            int v = Objects.requireNonNullElse(verbosiy.get(), 0);
            String[] args = {
                    javaExec,
                    "-classpath", systemClassPath,
                    "-jar", jarLocation,
                    "-i", cabeInputDir.toString(),
                    "-o", cabeOutputDir.toString(),
                    "-c", config.get().getConfigString(),
                    "-cp", classpath,
                    "-v", Integer.toString(v)
            };

            // Always log the command being executed
            if (logger.isInfoEnabled()) {
                logger.info("{}", String.join(" ", args));
            }
            ProcessBuilder pb = new ProcessBuilder(args);

            Process process = pb.start();

            try (CopyOutput copyStdErr = new CopyOutput(process.errorReader(), msg -> logger.warn("{}", msg));
                 CopyOutput copyStdOut = new CopyOutput(process.inputReader(), msg -> logger.info("{}", msg))) {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new GradleException("instrumenting class files failed\n\n" + copyStdErr);
                }
            }
        } catch (InterruptedException e) {
            logger.warn("instrumenting class files interrupted");
            throw e;
        } catch (Exception e) {
            throw new GradleException("An error occurred while instrumenting classes: " + e.getMessage(), e);
        }
    }

    /**
     * Copies `.class` files from the specified output directory to the specified input directory.
     * Preserves the directory structure while copying files. Only `.class` files are included in the operation.
     *
     * @param cabeOutputDir the directory containing the original class files to be copied
     * @param cabeInputDir the target directory where the class files will be copied
     * @throws GradleException if an I/O error occurs during the operation
     */
    private static void copyClassesToCabeInputDir(File cabeOutputDir, File cabeInputDir) {
        try (Stream<Path> classFileStream = Files.walk(cabeOutputDir.toPath())) {
            classFileStream
                    .filter(path -> path.toString().endsWith(".class"))
                    .forEach(source -> {
                        try {
                            Path target = cabeInputDir.toPath().resolve(cabeOutputDir.toPath().relativize(source));
                            Files.createDirectories(target.getParent());
                            Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new GradleException("Failed to copy class file: " + source, e);
                        }
                    });
        } catch (IOException e) {
            throw new GradleException("Failed to copy classes directory: " + cabeOutputDir.getAbsolutePath(), e);
        }
    }

    /**
     * This class is responsible for copies the output of a Reader to a specified Consumer
     * and stores the first 10 lines to be printed later using {@code }toString()}.
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
