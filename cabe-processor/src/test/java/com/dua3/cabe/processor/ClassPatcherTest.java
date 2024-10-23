package com.dua3.cabe.processor;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClassPatcherTest {
    private static final Logger LOG = Logger.getLogger(ClassPatcherTest.class.getName());

    static Path testDir = TestUtil.buildDir.resolve(ClassPatcherTest.class.getSimpleName());
    static Path testSrcDir = testDir.resolve("src");
    static Path testClassesUnprocessedDir = testDir.resolve("classes-unprocessed");
    static Path testClassesProcessedParameterInfoDir = testDir.resolve("classes-processed-parameterinfo");
    static Path testClassesProcessedInstrumentedDir = testDir.resolve("classes-processed-instrumented");
    static List<Path> classFiles = new ArrayList<>();


    @BeforeAll
    static void setUp() throws IOException {
        LOG.info("ClassPatcherTest.setUp()");

        // make sure the build and resource directories exist
        if (!Files.isDirectory(TestUtil.buildDir)) {
            throw new IllegalStateException("build directory not found: " + TestUtil.buildDir);
        }
        if (!Files.isDirectory(TestUtil.resourceDir)) {
            throw new IllegalStateException("resource directory not found: " + TestUtil.resourceDir);
        }

        // create directories
        LOG.info("creating directories ...");
        Files.createDirectories(testDir);
        Files.createDirectories(testClassesUnprocessedDir);
        Files.createDirectories(testClassesProcessedInstrumentedDir);

        // copy test source files
        LOG.info("copying test sources ...");
        TestUtil.copyRecursive(TestUtil.resourceDir.resolve("testsrc"), testSrcDir);

        // compile source files to classes-unprocessed folder
        LOG.info("compiling test sources ...");
        TestUtil.compileSources(testSrcDir, testClassesUnprocessedDir, TestUtil.resourceDir.resolve("testLib"));

        // Load classes into class pool
        LOG.info("loading class files ...");
        classFiles = TestUtil.loadClasses(testClassesUnprocessedDir);

        LOG.info("setup complete, " + classFiles.size() + " classes loaded");
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
            String className = TestUtil.getClassName(classFile);
            ClassInfo ci = ClassInfo.forClass(TestUtil.loader, className);
            CtClass ctClass = TestUtil.pool.getCtClass(ci.name());
            for (var mi : ci.methods()) {
                if (mi.isAbstract()) {
                    continue;
                }

                String methodName = mi.name();
                try (Formatter fmtCode = new Formatter()) {
                    for (var pi : mi.parameters()) {
                        if (pi.isSynthetic() || ci.isAnonymousClass() && mi.isConstructor()) {
                            continue;
                        }
                        switch (pi.type().getCanonicalName()) {
                            case "java.lang.String":
                                fmtCode.format("if (%2$s != null && !%2$s.getClass().getName().equals(\"java.lang.String\"))%n" +
                                                "  throw new java.lang.IllegalArgumentException(\"[%3$s] expected type String but was: '\" + %2$s.getClass().getName() + \"'\");%n",
                                        pi.name(), pi.param(), mi.name());
                                // if the parameter name contains a '#' then the actual name could not be extracted from the Jar
                                fmtCode.format("if (!\"%1$s\".contains(\"#\") && !\"%1$s\".equals(%2$s))%n" +
                                                "  throw new java.lang.IllegalArgumentException(\"[%3$s] expected: '%1$s', actual: '\"+%2$s+\"'\");%n",
                                        pi.name(), pi.param(), mi.name());
                                break;
                            case "java.lang.Object[]":
                                fmtCode.format("if (%2$s != null && !%2$s.getClass().getName().equals(java.lang.Object[].class.getName()))%n" +
                                                "  throw new java.lang.IllegalArgumentException(\"[%3$s] expected type Object[] but was: '\" + %2$s.getClass().getName() + \"'\");%n",
                                        pi.name(), pi.param(), mi.name());
                                // if the parameter name contains a '#' then the actual name could not be extracted from the Jar
                                fmtCode.format("if (!\"%1$s\".contains(\"#\") && !\"%1$s\".equals(java.lang.reflect.Array.get(%2$s, 0)))%n" +
                                                "  throw new java.lang.IllegalArgumentException(\"[%3$s] expected: '%1$s', actual: '\"+java.lang.reflect.Array.get(%2$s, 0)+\"'\");%n",
                                        pi.name(), pi.param(), mi.name());
                                break;
                            default:
                                throw new IllegalStateException("unexpected type: " + pi.type().getName() + " [" + methodName + "]");
                        }
                    }
                    String code = fmtCode.toString();
                    if (!code.isEmpty()) {
                        CtBehavior ctBehavior = ClassPatcher.getCtMethod(ctClass, mi);
                        ctBehavior.insertBefore(code);
                    }
                }
            }
            ctClass.writeFile(testClassesProcessedParameterInfoDir.toString());
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

    @Test
    @Order(3)
    void processFolder() {
        LOG.info("testing processFolder()");
        // processFolder will report errors by throwing an exception
        assertDoesNotThrow(() -> {
            Collection<Path> classPath = List.of();
            ClassPatcher patcher = new ClassPatcher(classPath, Configuration.StandardConfig.DEVELOPMENT.config());
            patcher.processFolder(testClassesUnprocessedDir, testClassesProcessedInstrumentedDir);
        });
    }

    @ParameterizedTest
    @Order(4)
    @ValueSource(strings = {
            "com.dua3.cabe.processor.test.instrument.NoAnnotations",
            "com.dua3.cabe.processor.test.instrument.ParameterAnnotations",
            "com.dua3.cabe.processor.test.instrument.ParameterAnnotationsStaticMethods",
            "com.dua3.cabe.processor.test.instrument.api.nullmarked.NullMarkedPackage",
            "com.dua3.cabe.processor.test.instrument.api.nullunmarked.NullUnmarkedPackage"
    })
    void testInstrumentation(String className) {
        LOG.info("testing correct results of instrumentation: " + className);
        // Each of the classes contains a test method that will throw an exception when an incorrect result is detected.
        assertDoesNotThrow(() -> {
            try (var cl = new URLClassLoader(new URL[]{testClassesProcessedInstrumentedDir.toUri().toURL()}, null)) {
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

    /**
     * Test generating null checks for different configurations. This test also makes sure that null checks for
     * {@link Configuration.Check#ASSERT} are work the same as standard assertions i.e., can be en-/disabled using the
     * standard JVM flags ('-ea and '-da').
     *
     * @param configName the name of the configuration as defined in {@link Configuration.StandardConfig}
     * @throws IOException                           if an I/O error occurs
     * @throws ClassFileProcessingFailedException    if processing of a class file fails
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "NO_CHECKS",
            "DEVELOPMENT",
            "STANDARD"
    })
    @Order(6)
    public void testConfiguration(String configName) throws IOException, ClassFileProcessingFailedException {
        Configuration.StandardConfig config = Configuration.StandardConfig.valueOf(configName);

        // create directories
        Path unprocessedDir = testDir.resolve("classes-unprocessed-" + configName);
        Path processedDir = testDir.resolve("classes-processed-" + configName);

        // copy sources
        TestUtil.copyRecursive(
                testClassesUnprocessedDir.resolve("com/dua3/cabe/processor/test/config"),
                unprocessedDir.resolve("com/dua3/cabe/processor/test/config")
        );

        // process classes
        TestUtil.processClasses(unprocessedDir, processedDir, config.config());

        // test processed classes
        try (Formatter fmt = new Formatter()) {
            String header = "Config: " + config.name();
            fmt.format("%s%n", header);
            fmt.format("%s%n", "=".repeat(header.length()));

            try (Stream<Path> pathStream = Files.walk(processedDir)) {
                String result = pathStream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".class"))
                        .filter(p -> !p.toString().contains("$")) // filter out anonymous classes
                        .map(path -> runAndTestProcessedClassesForConfig(processedDir, processedDir.relativize(path)))
                        .collect(Collectors.joining());
                fmt.format("%s", result);
            }

            String actual = fmt.toString();
            String expected = EXPECTED_FOR_CONFIG.getOrDefault(config, "");
            assertEquals(expected, actual, "failed: " + config);
        }
    }

    private static final Map<Configuration.StandardConfig, String> EXPECTED_FOR_CONFIG = Map.of(
            Configuration.StandardConfig.NO_CHECKS, """
                    Config: NO_CHECKS
                    =================
                    Testing com/dua3/cabe/processor/test/config/TestClass.class with assertions false
                    ---------------------------------------------------------------------------------
                    assertions enabled  : false
                    privateNullable     : -
                    privateNonNull      : -
                    publicNullable      : -
                    publicNonNull       : -
                                        
                    Testing com/dua3/cabe/processor/test/config/TestClass.class with assertions true
                    --------------------------------------------------------------------------------
                    assertions enabled  : true
                    privateNullable     : -
                    privateNonNull      : -
                    publicNullable      : -
                    publicNonNull       : -
                                        
                    Testing com/dua3/cabe/processor/test/config/TestClassStdAssert.class with assertions false
                    ------------------------------------------------------------------------------------------
                    assertions enabled  : false
                    privateNullable     : -
                    privateNonNull      : -
                    publicNullable      : -
                    publicNonNull       : java.lang.NullPointerException
                                        
                    Testing com/dua3/cabe/processor/test/config/TestClassStdAssert.class with assertions true
                    -----------------------------------------------------------------------------------------
                    assertions enabled  : true
                    privateNullable     : -
                    privateNonNull      : java.lang.AssertionError
                    publicNullable      : -
                    publicNonNull       : java.lang.NullPointerException
                                        
                    Testing com/dua3/cabe/processor/test/config/TestInterface.class with assertions false
                    -------------------------------------------------------------------------------------
                    assertions enabled  : false
                    privateNullable     : -
                    privateNonNull      : -
                    publicNullable      : -
                    publicNonNull       : -
                    publicNullableDefault: -
                    publicNonNullDefault: -
                                        
                    Testing com/dua3/cabe/processor/test/config/TestInterface.class with assertions true
                    ------------------------------------------------------------------------------------
                    assertions enabled  : true
                    privateNullable     : -
                    privateNonNull      : -
                    publicNullable      : -
                    publicNonNull       : -
                    publicNullableDefault: -
                    publicNonNullDefault: -
                                        
                    """,
            Configuration.StandardConfig.DEVELOPMENT, """
                    Config: DEVELOPMENT
                    ===================
                    Testing com/dua3/cabe/processor/test/config/TestClass.class with assertions false
                    ---------------------------------------------------------------------------------
                    assertions enabled  : false
                    privateNullable     : -
                    privateNonNull      : java.lang.AssertionError
                    publicNullable      : -
                    publicNonNull       : java.lang.AssertionError
                                        
                    Testing com/dua3/cabe/processor/test/config/TestClass.class with assertions true
                    --------------------------------------------------------------------------------
                    assertions enabled  : true
                    privateNullable     : -
                    privateNonNull      : java.lang.AssertionError
                    publicNullable      : -
                    publicNonNull       : java.lang.AssertionError
                                        
                    Testing com/dua3/cabe/processor/test/config/TestClassStdAssert.class with assertions false
                    ------------------------------------------------------------------------------------------
                    assertions enabled  : false
                    privateNullable     : -
                    privateNonNull      : -
                    publicNullable      : -
                    publicNonNull       : java.lang.NullPointerException
                                        
                    Testing com/dua3/cabe/processor/test/config/TestClassStdAssert.class with assertions true
                    -----------------------------------------------------------------------------------------
                    assertions enabled  : true
                    privateNullable     : -
                    privateNonNull      : java.lang.AssertionError
                    publicNullable      : -
                    publicNonNull       : java.lang.NullPointerException
                                        
                    Testing com/dua3/cabe/processor/test/config/TestInterface.class with assertions false
                    -------------------------------------------------------------------------------------
                    assertions enabled  : false
                    privateNullable     : -
                    privateNonNull      : java.lang.AssertionError
                    publicNullable      : -
                    publicNonNull       : java.lang.AssertionError
                    publicNullableDefault: -
                    publicNonNullDefault: java.lang.AssertionError
                                        
                    Testing com/dua3/cabe/processor/test/config/TestInterface.class with assertions true
                    ------------------------------------------------------------------------------------
                    assertions enabled  : true
                    privateNullable     : -
                    privateNonNull      : java.lang.AssertionError
                    publicNullable      : -
                    publicNonNull       : java.lang.AssertionError
                    publicNullableDefault: -
                    publicNonNullDefault: java.lang.AssertionError
                                        
                    """,
            Configuration.StandardConfig.STANDARD, """
                    Config: STANDARD
                    ================
                    Testing com/dua3/cabe/processor/test/config/TestClass.class with assertions false
                    ---------------------------------------------------------------------------------
                    assertions enabled  : false
                    privateNullable     : -
                    privateNonNull      : -
                    publicNullable      : -
                    publicNonNull       : java.lang.NullPointerException
                                        
                    Testing com/dua3/cabe/processor/test/config/TestClass.class with assertions true
                    --------------------------------------------------------------------------------
                    assertions enabled  : true
                    privateNullable     : -
                    privateNonNull      : java.lang.AssertionError
                    publicNullable      : -
                    publicNonNull       : java.lang.NullPointerException
                                        
                    Testing com/dua3/cabe/processor/test/config/TestClassStdAssert.class with assertions false
                    ------------------------------------------------------------------------------------------
                    assertions enabled  : false
                    privateNullable     : -
                    privateNonNull      : -
                    publicNullable      : -
                    publicNonNull       : java.lang.NullPointerException
                                        
                    Testing com/dua3/cabe/processor/test/config/TestClassStdAssert.class with assertions true
                    -----------------------------------------------------------------------------------------
                    assertions enabled  : true
                    privateNullable     : -
                    privateNonNull      : java.lang.AssertionError
                    publicNullable      : -
                    publicNonNull       : java.lang.NullPointerException
                                        
                    Testing com/dua3/cabe/processor/test/config/TestInterface.class with assertions false
                    -------------------------------------------------------------------------------------
                    assertions enabled  : false
                    privateNullable     : -
                    privateNonNull      : -
                    publicNullable      : -
                    publicNonNull       : java.lang.NullPointerException
                    publicNullableDefault: -
                    publicNonNullDefault: java.lang.NullPointerException
                                        
                    Testing com/dua3/cabe/processor/test/config/TestInterface.class with assertions true
                    ------------------------------------------------------------------------------------
                    assertions enabled  : true
                    privateNullable     : -
                    privateNonNull      : java.lang.AssertionError
                    publicNullable      : -
                    publicNonNull       : java.lang.NullPointerException
                    publicNullableDefault: -
                    publicNonNullDefault: java.lang.NullPointerException
                                        
                    """
    );

    private String runAndTestProcessedClassesForConfig(Path root, Path path) {
        String text;
        try (Formatter fmt = new Formatter()) {
            for (boolean assertionsEnabled : new boolean[]{false, true}) {
                String header = String.format("Testing %s with assertions %s", path, assertionsEnabled);
                fmt.format("%s%n", header);
                fmt.format("%s%n", "-".repeat(header.length()));

                String className = path.toString().replaceFirst(".class$", "");
                fmt.format("%s", TestUtil.runClass(root, className, assertionsEnabled));
            }
            text = fmt.toString();
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return text;
    }
}