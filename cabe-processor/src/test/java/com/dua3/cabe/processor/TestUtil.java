package com.dua3.cabe.processor;

import javassist.ClassPool;
import javassist.NotFoundException;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TestUtil {
    private static final Logger LOG = Logger.getLogger(TestUtil.class.getName());

    static final ClassPool pool = new ClassPool(true);
    static final Path buildDir = Paths.get(System.getProperty("user.dir")).resolve("build");
    static final Path resourceDir = Paths.get(System.getProperty("user.dir")).resolve("src/test/resources");

    private TestUtil() {}


    /**
     * Copy a folder from the source path to the destination path.
     *
     * @param src  the source path of the folder to be copied
     * @param dest the destination path where the folder will be copied
     * @throws IOException if an I/O error occurs during the copy operation
     */
    public static void copyRecursive(Path src, Path dest) throws IOException {
        try (Stream<Path> files = Files.walk(src)) {
            files.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
        }
    }

    /**
     * Copy a single file or directory (without contents) from the source path to the destination path.
     *
     * @param source the source path of the file or directory to be copied
     * @param dest   the destination path where the file or directory will be copied
     * @throws RuntimeException if an error occurs during the copy operation
     */
    public static void copy(Path source, Path dest) {
        try {
            if (Files.isDirectory(source)) {
                if (!Files.exists(dest)) {
                    Files.createDirectories(dest);
                }
            } else {
                Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Fetches all Java files in a given directory.
     *
     * @param dir the directory to search for Java files
     * @return an array of Path objects representing the Java files found in the directory
     * @throws IOException if an I/O error occurs while walking the directory
     */
    static Path[] fetchJavaFiles(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toArray(Path[]::new);
        }
    }

    /**
     * Returns the fully qualified class name of the given class file.
     *
     * @param classFile the path to the class file
     * @return the fully qualified class name
     */
    static String getClassName(Path classFile) {
        return classFile.toString()
                .replaceFirst("\\.[^.]*$", "")
                .replace(File.separatorChar, '.');
    }

    /**
     * Compiles Java source files into classes using the system Java compiler.
     *
     * @param srcDir     the directory containing the Java source files to compile
     * @param classesDir the directory where the compiled classes will be stored
     * @param libDir     the directory containing any required library files
     * @throws IOException if an I/O error occurs during the compilation process
     * @throws IllegalStateException if the compilation fails
     */
    public static void compileSources(Path srcDir, Path classesDir, Path libDir) throws IOException {
        Files.createDirectories(classesDir);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> options = List.of(
                "-d", classesDir.toString(),
                "-p", libDir.toString(),
                "-proc:full",
                "-g"
        );
        JavaCompiler.CompilationTask task = compiler.getTask(
                null,   // writer for additional output from the compiler; use System.err if null.
                null,       // file manager; if null, use compiler's standard file manager
                null,       // diagnostic listener; if null use compiler's default method for reporting diagnostics
                options,    // options to the compiler
                null,       // classes to be processed by annotation processing, null means process all
                compiler.getStandardFileManager(null, null, null)
                        .getJavaFileObjects(TestUtil.fetchJavaFiles(srcDir)) // source files to compile
        );
        boolean success = task.call();
        if (!success) {
            throw new IllegalStateException("Compilation of test sources failed.");
        }
    }

    /**
     * Loads all class files from a directory into the class pool and returns the list of class file paths.
     *
     * @param dir the directory containing the class files
     * @return a list of Path objects representing the class files
     * @throws IOException if an I/O error occurs while loading the class files
     */
    public static List<Path> loadClasses(Path dir) throws IOException {
        // Load classes into class pool
        Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .forEach(cp -> {
                    try {
                        pool.appendClassPath(cp);
                    } catch (NotFoundException e) {
                        LOG.warning("could not add to classpath: " + cp);
                    }
                });

        try {
            pool.appendClassPath(dir.toString());
        } catch (NotFoundException e) {
            throw new IllegalStateException("could not append classes folder to classpath: " + dir, e);
        }

        try (Stream<Path> paths = Files.walk(dir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".class"))
                    .map(dir::relativize)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Process classes from an unprocessed directory and patch them using the provided configuration.
     * The processed classes will be saved in a processed directory.
     *
     * @param unprocessedDir the directory containing the unprocessed classes
     * @param processedDir   the directory where the processed classes will be saved
     * @param config         the configuration to use for patching the classes
     * @throws IOException                            if an I/O error occurs during the processing or saving of classes
     * @throws ClassFileProcessingFailedException     if the processing of a class file fails
     */
    public static void processClasses(Path unprocessedDir, Path processedDir, Config config) throws IOException, ClassFileProcessingFailedException {
        Collection<Path> classPath = List.of();
        ClassPatcher patcher = new ClassPatcher(classPath, config);
        patcher.processFolder(unprocessedDir, processedDir);
    }

    public static String runClass(Path dir, String className, boolean assertionsEnabled) throws IOException, InterruptedException {
        ProcessBuilder processBuilder =
                new ProcessBuilder("java", (assertionsEnabled ? "-ea" : "-da"), className)
                        .directory(dir.toFile());
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            return new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        } else {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
