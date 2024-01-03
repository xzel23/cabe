package com.dua3.cabe.processor;

import javassist.ClassPool;
import javassist.NotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClassPatcherTest {
    private static final Logger LOG = Logger.getLogger(ClassPatcherTest.class.getName());

    static Path buildDir = Paths.get(System.getProperty("user.dir")).resolve("build");
    static Path resourceDir = Paths.get(System.getProperty("user.dir")).resolve("src/test/resources");
    static Path testDir = buildDir.resolve(ClassPatcherTest.class.getSimpleName());
    static Path testSrcDir = testDir.resolve("src");
    static Path testClassesUnprocessedDir = testDir.resolve("classes-unprocessed");
    static Path testClassesProcessedParameterInfoDir = testDir.resolve("classes-processed-parameterinfo");
    static Path testClassesProcessedInstrumentedDir = testDir.resolve("classes-processed-instrumented");
    static ClassPool pool = new ClassPool(true);
    static List<Path> classFiles = new ArrayList<>();


    @BeforeAll
    static void setUp() throws IOException {
        LOG.info("ClassPatcherTest.setUp()");

        // make sure the build and resource directories exist
        if (!Files.isDirectory(buildDir)) {
            throw new IllegalStateException("build directory not found: " + buildDir);
        }
        if (!Files.isDirectory(resourceDir)) {
            throw new IllegalStateException("resource directory not found: " + resourceDir);
        }

        // create directories
        LOG.info("creating directories ...");
        Files.createDirectories(testDir);
        Files.createDirectories(testSrcDir);
        Files.createDirectories(testClassesUnprocessedDir);
        Files.createDirectories(testClassesProcessedInstrumentedDir);

        // copy test source files
        LOG.info("copying test sources ...");
        copyFolder(resourceDir.resolve("testsrc"), testSrcDir);

        // compile source files to classes-unprocessed folder
        LOG.info("compiling test sources ...");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> options = List.of(
                "-d", testClassesUnprocessedDir.toString(),
                "-p", resourceDir.resolve("testLib").toString(),
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
                        .getJavaFileObjects(fetchJavaFiles(testSrcDir)) // source files to compile
        );
        boolean success = task.call();
        if (!success) {
            throw new IllegalStateException("Compilation of test sources failed.");
        }

        // Load classes into class pool
        LOG.info("loading class files ...");
        Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .forEach(cp -> {
                    try {
                        pool.appendClassPath(cp);
                    } catch (NotFoundException e) {
                        LOG.warning("could not add to classpath: " + cp);
                    }
                });

        try {
            pool.appendClassPath(testClassesUnprocessedDir.toString());
        } catch (NotFoundException e) {
            throw new IllegalStateException("could not append classes folder to classpath: " + testSrcDir, e);
        }

        classFiles.clear();
        try (Stream<Path> paths = Files.walk(testClassesUnprocessedDir)) {
            classFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".class"))
                    .map(f -> testClassesUnprocessedDir.relativize(f))
                    .collect(Collectors.toList());
        }

        LOG.info("setup complete, " + classFiles.size() + " classes loaded");
    }

    private static void copyFolder(Path src, Path dest) throws IOException {
        try (Stream<Path> files = Files.walk(src)) {
            files.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
        }
    }

    private static void copy(Path source, Path dest) {
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

    private static Path[] fetchJavaFiles(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toArray(Path[]::new);
        }
    }

    private static Stream<Path> parameterinfoClassFiles() {
        return classFiles.stream().filter(f -> f.toString().contains("parameterinfo"));
    }

    @ParameterizedTest
    @Order(1)
    @MethodSource("parameterinfoClassFiles")
    void testCallParameterTypes(Path classFile) {
        LOG.info("testing getParameterInfo(): " + classFile);
        // The code does call  getParameterInfo() for all constructors and methods.
        // Failure is signaled by a thrown exception
        assertDoesNotThrow(() -> {
            String className = getClassName(classFile);
            ClassInfo ci = ClassInfo.forClass(pool, className);
            for (var mi : ci.methods()) {
                String methodName = mi.name();
                try (Formatter fmtCode = new Formatter()) {
                    for (var pi : mi.parameters()) {
                        if (pi.isSynthetic() || ci.isAnonymousClass() && mi.isConstructor()) {
                            continue;
                        }
                        if (mi.name().equals("com.dua3.cabe.processor.test.parameterinfo.Base$1(java.lang.String,java.lang.String)")) {
                            // arguments for the inner class constructor are synthetic and names are not available
                            switch (pi.type()) {
                                case "java.lang.String":
                                    fmtCode.format("if (%2$s != null && !%2$s.getClass().getName().equals(\"java.lang.String\"))" +
                                                    "  throw new java.lang.IllegalArgumentException(\"[%3$s] expected type String but was: '\" + %2$s.getClass().getName() + \"'\");%n",
                                            pi.name(), pi.param(), mi.name());
                                    fmtCode.format("if (!\"%1$s\".equals(%2$s))" +
                                                    "  throw new java.lang.IllegalArgumentException(\"[%3$s] expected: '%1$s', actual: '\"+%2$s+\"'\");%n",
                                            pi.name().replace("arg", "param#"), pi.param(), mi.name());
                                    break;
                                case "java.lang.Object[]":
                                    fmtCode.format("if (%2$s != null && !%2$s.getClass().getName().equals(java.lang.Object[].class.getName()))%n" +
                                                    "  throw new java.lang.IllegalArgumentException(\"[%3$s] expected type Object[] but was: '\" + %2$s.getClass().getName() + \"'\");%n",
                                            pi.name(), pi.param(), mi.name());
                                    fmtCode.format("if (!\"%1$s\".equals(java.lang.reflect.Array.get(%2$s, 0)))" +
                                                    "  throw new java.lang.IllegalArgumentException(\"[%3$s] expected: '%1$s', actual: '\"+java.lang.reflect.Array.get(%2$s, 0)+\"'\");%n",
                                            pi.name().replace("arg", "param#"), pi.param(), mi.name());
                                    break;
                                default:
                                    throw new IllegalStateException("unexpected type: " + pi.type());
                            }
                        } else {
                            switch (pi.type()) {
                                case "java.lang.String":
                                    fmtCode.format("if (%2$s != null && !%2$s.getClass().getName().equals(\"java.lang.String\"))%n" +
                                                    "  throw new java.lang.IllegalArgumentException(\"[%3$s] expected type String but was: '\" + %2$s.getClass().getName() + \"'\");%n",
                                            pi.name(), pi.param(), mi.name());
                                    fmtCode.format("if (!\"%1$s\".equals(%2$s))%n" +
                                                    "  throw new java.lang.IllegalArgumentException(\"[%3$s] expected: '%1$s', actual: '\"+%2$s+\"'\");%n",
                                            pi.name(), pi.param(), mi.name());
                                    break;
                                case "java.lang.Object[]":
                                    fmtCode.format("if (%2$s != null && !%2$s.getClass().getName().equals(java.lang.Object[].class.getName()))%n" +
                                                    "  throw new java.lang.IllegalArgumentException(\"[%3$s] expected type Object[] but was: '\" + %2$s.getClass().getName() + \"'\");%n",
                                            pi.name(), pi.param(), mi.name());
                                    fmtCode.format("if (!\"%1$s\".equals(java.lang.reflect.Array.get(%2$s, 0)))%n" +
                                                    "  throw new java.lang.IllegalArgumentException(\"[%3$s] expected: '%1$s', actual: '\"+java.lang.reflect.Array.get(%2$s, 0)+\"'\");%n",
                                            pi.name(), pi.param(), mi.name());
                                    break;
                                default:
                                    throw new IllegalStateException("unexpected type: " + pi.type() + " [" + methodName + "]");
                            }
                        }
                    }
                    String code = fmtCode.toString();
                    if (!code.isEmpty()) {
                        mi.ctMethod().insertBefore(code);
                    }
                }
            }
            ci.ctClass().writeFile(testClassesProcessedParameterInfoDir.toString());
        }, "failed: " + classFile);
    }

    @ParameterizedTest
    @Order(2)
    @ValueSource(strings = {
            "com.dua3.cabe.processor.test.parameterinfo.Base",
            "com.dua3.cabe.processor.test.parameterinfo.Derived",
            "com.dua3.cabe.processor.test.parameterinfo.Implements",
            "com.dua3.cabe.processor.test.parameterinfo.Interface"
    })
    void testResultParameterTypes(String className) {
        LOG.info("testing correct results of getParameterInfo(): " + className);
        // The code does call  getParameterInfo() for all constructors and methods.
        // During the previous test step, assertions have been injected into the code to check the parameter values.
        // Methods are called with string arguments "arg1", "arg2" and so on.
        // The generated assertions check that the mapping from internal Javassist variables $1, $2, $3, ... to the
        // parameter names is correct by asserting that the internal variable $n contains the value "argn".
        // Failure is signaled by a thrown exception.
        assertDoesNotThrow(() -> {
            try (var cl = new URLClassLoader(new URL[]{testClassesProcessedParameterInfoDir.toUri().toURL()})) {
                Class<?> cls = cl.loadClass(className);
                var test = cls.getDeclaredMethod("test");
                test.invoke(null);
            }
        }, "failed: " + className);
    }

    private static String getClassName(Path classFile) {
        return classFile.toString()
                .replaceFirst("\\.[^.]*$", "")
                .replace(File.separatorChar, '.');
    }

    @Test
    @Order(3)
    void processFolder() {
        LOG.info("testing processFolder()");
        // processFolder will report errors by throwing an exception
        assertDoesNotThrow(() -> {
            Collection<Path> classPath = List.of();
            ClassPatcher patcher = new ClassPatcher(classPath);
            patcher.processFolder(testClassesUnprocessedDir, testClassesProcessedInstrumentedDir);
        });
    }

    @ParameterizedTest
    @Order(4)
    @ValueSource(strings = {
            "com.dua3.cabe.processor.test.instrument.ParameterAnnotations",
            "com.dua3.cabe.processor.test.instrument.ParameterAnnotationsStaticMethods",
            "com.dua3.cabe.processor.test.instrument.api.notnull.NotNullPackage",
            "com.dua3.cabe.processor.test.instrument.api.nullable.NullablePackage"
    })
    void testInstrumentation(String className) {
        LOG.info("testing correct results of instrumentation: " + className);
        // Each of the classes contains a test method that will throw an exception when an incorrect result is detected.
        assertDoesNotThrow(() -> {
            try (var cl = new URLClassLoader(new URL[]{testClassesProcessedInstrumentedDir.toUri().toURL()})) {
                Class<?> cls = cl.loadClass(className);
                var test = cls.getDeclaredMethod("test");
                test.invoke(null);
            }
        }, "Failed to instrument " + className);
    }

    @Test
    @Order(5)
    void testInstrumentationModuleInfo() {
        assertTrue(Files.isRegularFile(testClassesProcessedInstrumentedDir.resolve("module-info.class")), "module-info.class is missing");
    }
}