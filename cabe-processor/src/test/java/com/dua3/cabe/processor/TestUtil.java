package com.dua3.cabe.processor;

import javassist.ClassPool;
import javassist.NotFoundException;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class TestUtil {
    private static final Logger LOG = Logger.getLogger(TestUtil.class.getName());

    static ClassLoader loader;
    static final ClassPool pool = new ClassPool(true);
    static final Path buildDir = Paths.get(System.getProperty("cabe.test.build.dir", System.getProperty("user.dir") + File.separator + "build"));
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
        String fileName = classFile.toString();

        if (!fileName.endsWith(".class")) {
            throw new IllegalStateException("not a class file name: " + fileName);
        }
        fileName = fileName.substring(0, fileName.length() - ".class".length());

        return fileName.replace('/', '.').replace('\\', '.');
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
                "-g",
                "-parameters"
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
     * Compiles Java source files using the javac executable from a specific Java installation.
     *
     * @param javaHome   the Java installation to use
     * @param release    the javac --release value
     * @param srcDir     the directory containing the Java source files to compile
     * @param classesDir the directory where the compiled classes will be stored
     * @param libDir     the directory containing any required library files
     * @throws IOException if an I/O error occurs during the compilation process
     * @throws InterruptedException if the current thread is interrupted while waiting for javac
     */
    public static void compileSources(Path javaHome, int release, Path srcDir, Path classesDir, Path libDir)
            throws IOException, InterruptedException {
        compileSources(javaHome, release, srcDir, classesDir, libDir, List.of("-g", "-parameters"));
    }

    /**
     * Compiles Java source files using the javac executable from a specific Java installation.
     *
     * @param javaHome   the Java installation to use
     * @param release    the javac --release value
     * @param srcDir     the directory containing the Java source files to compile
     * @param classesDir the directory where the compiled classes will be stored
     * @param libDir     the directory containing any required library files
     * @param javacOptions additional javac options
     * @throws IOException if an I/O error occurs during the compilation process
     * @throws InterruptedException if the current thread is interrupted while waiting for javac
     */
    public static void compileSources(Path javaHome, int release, Path srcDir, Path classesDir, Path libDir, List<String> javacOptions)
            throws IOException, InterruptedException {
        Files.createDirectories(classesDir);
        List<String> command = new ArrayList<>();
        command.add(javaHome.resolve("bin").resolve(isWindows() ? "javac.exe" : "javac").toString());
        command.add("--release");
        command.add(Integer.toString(release));
        command.add("-d");
        command.add(classesDir.toString());
        command.add("-cp");
        command.add(libDir.resolve("jspecify-1.0.0.jar").toString());
        command.addAll(javacOptions);
        Arrays.stream(fetchJavaFiles(srcDir)).map(Path::toString).forEach(command::add);

        Process process = new ProcessBuilder(command).start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String err = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new IllegalStateException("Compilation of test sources failed.%n%s%n%s".formatted(out, err));
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
        List<URL> urls = new ArrayList<>();
        Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .forEach(cp -> {
                    try {
                        urls.add(new File(cp).toURI().toURL());
                        pool.appendClassPath(cp);
                    } catch (NotFoundException | MalformedURLException e) {
                        LOG.warning("could not add to classpath: " + cp);
                    }
                });

        try {
            urls.add(dir.toUri().toURL());
            pool.appendClassPath(dir.toString());
        } catch (NotFoundException e) {
            throw new IllegalStateException("could not append classes folder to classpath: " + dir, e);
        }
        loader = new URLClassLoader(urls.toArray(URL[]::new));

        try (Stream<Path> paths = Files.walk(dir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(f -> String.valueOf(f.getFileName()).endsWith(".class"))
                    .map(dir::relativize)
                    .toList();
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
    public static void processClasses(Path unprocessedDir, Path processedDir, Configuration config) throws IOException, ClassFileProcessingFailedException {
        Collection<Path> classPath = List.of(resourceDir.resolve("testLib"));
        ClassPatcher patcher = new ClassPatcher(classPath, config);
        patcher.processFolder(unprocessedDir, processedDir);
    }

    /**
     * Executes a Java class in a specified directory with the option to enable or disable assertions.
     * Takes into account JavaFX modules if their path is configured via the system property "org.openjfx.javafxplugin.path".
     * Returns the process's output if the exit code is zero, or the error output otherwise.
     *
     * @param dir               the directory from which to execute the Java class
     * @param className         the fully qualified name of the class to execute
     * @param assertionsEnabled a flag indicating whether assertions should be enabled (-ea) or disabled (-da) during execution
     * @return the standard output of the process if successful, or the error output otherwise
     * @throws IOException          if an I/O error occurs while starting the process
     * @throws InterruptedException if the current thread is interrupted while waiting for the process to terminate
     */
    public static String runClass(Path dir, String className, boolean assertionsEnabled) throws IOException, InterruptedException {
        String javaHome = System.getProperty("java.home");
        return runClass(Path.of(javaHome), dir, List.of(), className, assertionsEnabled);
    }

    /**
     * Executes a Java class using a specific Java installation.
     *
     * @param javaHome          the Java installation to use
     * @param dir               the working directory and first classpath entry
     * @param classpathEntries  additional classpath entries
     * @param className         the fully qualified name of the class to execute
     * @param assertionsEnabled a flag indicating whether assertions should be enabled
     * @return the standard output of the process if successful, or the error output otherwise
     * @throws IOException          if an I/O error occurs while starting the process
     * @throws InterruptedException if the current thread is interrupted while waiting for the process to terminate
     */
    public static String runClass(Path javaHome, Path dir, Collection<Path> classpathEntries, String className, boolean assertionsEnabled)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        String javaExecutable = javaHome.resolve("bin").resolve(isWindows() ? "java.exe" : "java").toString();
        command.add(javaExecutable);
        command.add(assertionsEnabled ? "-ea" : "-da");

        List<Path> classpath = new ArrayList<>();
        classpath.add(dir);
        classpath.addAll(classpathEntries);
        command.add("-cp");
        command.add(classpath.stream().map(Path::toString).collect(java.util.stream.Collectors.joining(File.pathSeparator)));

        // Add JavaFX modules to the module path for tests
        String javaFxPath = System.getProperty("org.openjfx.javafxplugin.path");
        if (javaFxPath != null && !javaFxPath.isEmpty()) {
            command.add("--module-path");
            command.add(javaFxPath);
            command.add("--add-modules");
            command.add("javafx.controls");
        }

        command.add(className);

        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(dir.toFile());
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            return new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        } else {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Reads the major version from a class file.
     *
     * @param classFile the class file
     * @return the class file major version
     * @throws IOException if an I/O error occurs
     */
    public static int readClassFileMajorVersion(Path classFile) throws IOException {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(classFile))) {
            int magic = in.readInt();
            if (magic != 0xCAFEBABE) {
                throw new IllegalArgumentException("not a class file: " + classFile);
            }
            in.readUnsignedShort();
            return in.readUnsignedShort();
        }
    }

    /**
     * Finds a Java installation for a specific feature version.
     *
     * @param featureVersion the Java feature version
     * @return the Java home, if found
     */
    public static Optional<Path> findJavaHome(int featureVersion) {
        String[] propertyNames = {
                "cabe.test.java" + featureVersion + ".home",
                "java" + featureVersion + ".home"
        };
        for (String propertyName : propertyNames) {
            String value = System.getProperty(propertyName);
            if (isJavaHome(value, featureVersion)) {
                return Optional.of(Path.of(value));
            }
        }

        String[] environmentNames = {
                "JAVA" + featureVersion + "_HOME",
                "JDK" + featureVersion + "_HOME"
        };
        for (String environmentName : environmentNames) {
            String value = System.getenv(environmentName);
            if (isJavaHome(value, featureVersion)) {
                return Optional.of(Path.of(value));
            }
        }

        Path currentJavaHome = Path.of(System.getProperty("java.home"));
        if (isJavaHome(currentJavaHome.toString(), featureVersion)) {
            return Optional.of(currentJavaHome);
        }

        if (!isWindows()) {
            try {
                Process process = new ProcessBuilder("/usr/libexec/java_home", "-v", Integer.toString(featureVersion)).start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    String javaHome = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
                    if (isJavaHome(javaHome, featureVersion)) {
                        return Optional.of(Path.of(javaHome));
                    }
                }
            } catch (IOException e) {
                LOG.fine(() -> "could not run /usr/libexec/java_home: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return Optional.empty();
    }

    private static boolean isJavaHome(String value, int featureVersion) {
        if (value == null || value.isBlank()) {
            return false;
        }
        Path java = Path.of(value).resolve("bin").resolve(isWindows() ? "java.exe" : "java");
        if (!Files.isExecutable(java)) {
            return false;
        }
        try {
            Process process = new ProcessBuilder(java.toString(), "-version").start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return false;
            }
            String versionText = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8)
                    + new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return versionText.contains("\"" + featureVersion + ".")
                    || versionText.contains("version \"" + featureVersion + "\"");
        } catch (IOException e) {
            LOG.fine(() -> "could not check Java home " + value + ": " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * Retrieves the parent path of the given {@code path}.
     * If the parent path is {@code null}, an exception is thrown with a descriptive error message.
     *
     * @param path the path whose parent is to be retrieved; must not be {@code null}.
     * @return the parent path of the given {@code path}; never {@code null}.
     * @throws NullPointerException if the given {@code path} is {@code null} or its parent path is {@code null}.
     */
    static Path getParentOrThrow(Path path) {
        return Objects.requireNonNull(path.getParent(), "parent path must not be null: " + path);
    }
}
