package com.dua3.cabe.processor;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The ClassPatcher class is responsible for patching class files by adding assertions for nullability checks
 * on method parameters. It collects information about the class file, including package information and
 * parameter annotations, and modifies the class file by injecting the necessary code.
 */
public class ClassPatcher {

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ClassPatcher.class.getName());

    /**
     * This method is the entry point of the application.
     *
     * @param args an array of command-line arguments
     */
    public static void main(String[] args) {
        Logger rootLogger = Logger.getLogger("");
        Handler consoleHandler = null;

        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                consoleHandler = handler;
                break;
            }
        }

        if (consoleHandler == null) {
            consoleHandler = new ConsoleHandler();
            rootLogger.addHandler(consoleHandler);
        }

        consoleHandler.setLevel(Level.FINEST);
        LOG.setLevel(Level.ALL);

        LOG.fine(() -> "args: %s".formatted(String.join("\n    ", args)));

        Path in = null;
        Path out = null;
        List<Path> classPaths = null;
        Configuration configuration = null;

        BitSet usedArgs = new BitSet(args.length);
        try {
            List<String> cmdLine = List.of(args);
            if (cmdLine.contains("--help")) {
                help();
                return;
            }

            String inputFolder = getOptionString(cmdLine, "-i", usedArgs);
            String outputFolder = getOptionString(cmdLine, "-o", usedArgs);
            String configStr = getOptionString(cmdLine, "-c", usedArgs, "standard");
            String classpath = getOptionString(cmdLine, "-cp", usedArgs, "");

            configuration = Configuration.parseConfigString(configStr);

            in = Paths.get(inputFolder);
            out = Paths.get(outputFolder);
            classPaths = Arrays.stream(classpath.split(File.pathSeparator)).map(Paths::get).toList();
        } catch (RuntimeException e) {
            System.err.println("Commandline error: " + e.getMessage());
            System.err.println("Command Arguments: " + Arrays.stream(args)
                    .map(s -> "\"" + s.replaceAll("\"", "\\\"") + "\"")
                    .collect(Collectors.joining(" ")));
            System.exit(1);
        }

        // check that all arguments have been processed
        for (int i = 0; i < args.length; i++) {
            if (!usedArgs.get(i)) {
                System.err.println("Unexpected argument: " + args[i]);
                System.exit(2);
            }
        }

        try {
            ClassPatcher classPatcher = new ClassPatcher(classPaths, configuration);
            classPatcher.processFolder(in, out);
        } catch (RuntimeException | IOException | ClassFileProcessingFailedException e) {
            LOG.log(Level.SEVERE, "Error processing class files", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
        }
    }

    private static String messageOptionNotFound(String option) {
        return String.format("Option '%s' not found at expected position", option);
    }

    private static String getOptionString(List<String> cmdLine, String option, BitSet usedArgs) {
        String value = getOptionString(cmdLine, option, usedArgs, null);
        return Objects.requireNonNull(value, () -> messageOptionNotFound(option));
    }

    private static String getOptionString(List<String> cmdLine, String option, BitSet usedArgs, String defaultValue) {
        int idxInput = cmdLine.indexOf(option);
        if (idxInput < 0) {
            return defaultValue;
        }

        if (usedArgs.get(idxInput)) {
            throw new IllegalArgumentException("Could not parse the command line at '" + cmdLine.get(idxInput) + "'");
        }
        usedArgs.set(idxInput);

        if (usedArgs.get(idxInput + 1)) {
            throw new IllegalArgumentException("Missing argument to option '" + cmdLine.get(idxInput) + "'");
        }
        usedArgs.set(idxInput + 1);

        return cmdLine.get(idxInput + 1);
    }

    private static void help() {
        String msg = """
                ClassPatcher
                ============
                                
                Add null checks in Java class file byte code.
                                
                Usage: java -jar <jar-file> -i <input-folder> -o <output-folder> [-c <configuration>] [-cp <classpath>]
                                
                    <configurations> : standard|development|no-checks (default: standard)
                    
                                       'standard'    - use standard assertions for private API methods,
                                                       throw NullPointerException for public API methods 
                                       'development' - failed checks will always throw an AssertionError 
                                       'no-check'    - do not add any null checks
                """;
        System.out.println(msg);
    }

    /**
     * Regular expression pattern for matching Fully Qualified Class Names (FQCN).
     * FQCN is a string representing the package and class name of a Java class.
     * The pattern matches valid Java identifiers separated by dots, allowing for nested classes.
     * It does not match reserved keywords or invalid identifier characters.
     */
    private static final Pattern PATTERN_FQCN = Pattern.compile("^(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\$\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*$");

    private static final Pattern GET_CLASS_NAME_PATTERN = Pattern.compile("\\.[^.]*$");

    private final List<Path> classpath;
    private final Configuration configuration;
    private ClassLoader classLoader;
    private ClassPool classPool;
    private Path inputFolder;
    private Path outputFolder;

    /**
     * This class represents a ClassPatcher object that manipulates class files in a given classpath.
     *
     * @param classpath     the compile-classpath
     * @param configuration the {@link Configuration} to use
     */
    public ClassPatcher(Collection<Path> classpath, Configuration configuration) {
        this.classpath = new ArrayList<>(Objects.requireNonNull(classpath, "classpath is null"));
        this.configuration = Objects.requireNonNull(configuration, "configuration is null");
    }

    /**
     * Process a folder containing class files.
     *
     * @param inputFolder  the folder to process
     * @param outputFolder the folder to write the patched files to
     * @throws IOException                        if an I/O error occurs
     * @throws ClassFileProcessingFailedException if processing of a class file fails
     */
    public synchronized void processFolder(Path inputFolder, Path outputFolder) throws IOException, ClassFileProcessingFailedException {
        try {
            LOG.fine(() -> "process folder " + inputFolder);

            this.inputFolder = Objects.requireNonNull(inputFolder, "input folder is null");
            this.outputFolder = Objects.requireNonNull(outputFolder, "output folder is null");
            this.classPool = new ClassPool(true);

            // no directory
            if (!Files.isDirectory(inputFolder)) {
                LOG.warning("does not exist or is not a directory: " + inputFolder);
                return;
            }

            List<Path> currentClasspath = new ArrayList<>(classpath);
            List<URL> classpathUrls = new ArrayList<>();
            currentClasspath.add(inputFolder);

            currentClasspath.forEach(cp -> {
                try {
                    classPool.appendClassPath(cp.toString());
                    classpathUrls.add(cp.toUri().toURL());
                } catch (NotFoundException e) {
                    LOG.warning("could not add to class pool: " + cp);
                } catch (MalformedURLException e) {
                    LOG.warning("could not convert to URL: " + cp);
                }
            });

            ModuleClassLoader moduleClassLoader = new ModuleClassLoader(ClassLoader.getSystemClassLoader(), currentClasspath.toArray(Path[]::new));
            this.classLoader = new URLClassLoader(classpathUrls.toArray(URL[]::new), moduleClassLoader);

            List<Path> classFiles;
            try (Stream<Path> paths = Files.walk(inputFolder)) {
                classFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(f -> f.getFileName().toString().endsWith(".class"))
                        .collect(Collectors.toList());
            }

            if (classFiles.isEmpty()) {
                LOG.info("no class files!");
                return;
            }

            processClassFiles(classFiles);
        } finally {
            this.inputFolder = null;
            this.classPool = null;
        }
    }

    /**
     * Processes a list of class files by calling the {@link #instrumentClassFile(Path)} method for each file.
     *
     * @param classFiles the list of class files to process
     * @throws IOException                        if an I/O error occurs
     * @throws ClassFileProcessingFailedException if processing of a class file fails
     */
    private void processClassFiles(List<Path> classFiles) throws IOException, ClassFileProcessingFailedException {
        for (Path classFile : classFiles) {
            instrumentClassFile(classFile);
        }
    }

    /**
     * Instruments a class file by adding null-check assertions for method parameters.
     *
     * @param classFile the path to the class file to be instrumented
     * @throws ClassFileProcessingFailedException if processing of the class file fails
     * @throws IOException                        if an I/O error occurs
     */
    private void instrumentClassFile(Path classFile) throws ClassFileProcessingFailedException, IOException {
        LOG.info(() -> "Instrumenting class file: " + classFile);

        Files.createDirectories(outputFolder.resolve(inputFolder.relativize(classFile.getParent())));

        String className = getClassName(classFile);
        LOG.fine(() -> "Class " + className);

        if (!PATTERN_FQCN.matcher(className).matches()) {
            if (!className.equals("module-info") && !className.endsWith(".package-info")) {
                LOG.warning(() -> "unusual class file name: " + classFile.getFileName() + " [" + classFile + "]");
            }

            Path target = outputFolder.resolve(inputFolder.relativize(classFile));
            LOG.fine(() -> "copying unchanged: " + classFile + " -> " + target);
            Files.copy(classFile, target, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        try {
            ClassInfo classInfo = ClassInfo.forClass(classLoader.loadClass(className));
            CtClass ctClass = classPool.getCtClass(className);
            try {
                for (var methodInfo : classInfo.methods()) {
                    try {
                        instrumentMethod(classInfo, methodInfo);
                    } finally {
                        ctClass.defrost();
                    }
                }

                // Write the class file
                LOG.fine("writing class file: " + classFile);
                Files.createDirectories(outputFolder.resolve(inputFolder.relativize(classFile.getParent())));
                ctClass.writeFile(outputFolder.toString());

                LOG.fine("instrumenting class file successful: " + classFile);
            } finally {
                ctClass.detach();
            }
        } catch (IOException e) {
            throw new IOException("IOException while instrumenting class file " + classFile, e);
        } catch (Exception e) {
            throw new ClassFileProcessingFailedException("instrumenting failed for class file " + classFile, e);
        }
    }

    /**
     * Retrieves the expression that represents whether assertions are enabled or disabled for a given class.
     *
     * @param ci the ClassInfo object representing the class
     * @return the assertion enabled expression as a String
     */
    private String getAssertionEnabledExpression(ClassInfo ci) throws CannotCompileException, NotFoundException {
        String assertionsDisabledFlagName = ci.assertionsDisabledFlagName();
        if (assertionsDisabledFlagName != null) {
            return "!" + assertionsDisabledFlagName;
        } else {
            // flag is not present in the unprocessed class file
            CtClass ctClass = classPool.getCtClass(ci.name());
            String flagName;
            if (Arrays.stream(ctClass.getDeclaredFields()).anyMatch(f -> f.getName().equals("$assertionsDisabled"))) {
                // the field has already been injected
                flagName = ctClass.getName() + ".$assertionsDisabled";
            } else {
                // inject directly into the current class
                LOG.fine(() -> "injecting field $assertionsDisabled in class: " + ci.name());
                CtField field = new CtField(CtClass.booleanType, "$assertionsDisabled", ctClass);
                int modifiers = ctClass.isInterface()
                        ? Modifier.STATIC | Modifier.FINAL | Modifier.PUBLIC | AccessFlag.SYNTHETIC
                        : Modifier.STATIC | Modifier.FINAL | AccessFlag.SYNTHETIC;
                field.setModifiers(modifiers);
                ctClass.addField(field);
                // also make sure the field is initialized correctly
                CtConstructor initializer = ctClass.getClassInitializer();
                String initializercode = "{ $assertionsDisabled = !" + ctClass.getName() + ".class.desiredAssertionStatus(); }";
                if (initializer == null) {
                    initializer = ctClass.makeClassInitializer();
                    initializer.setBody(initializercode);
                } else {
                    initializer.insertBefore(initializercode);
                }
                ctClass.defrost();

                // finally return the flag name
                flagName = ctClass.getName() + ".$assertionsDisabled";
            }
            return "!" + flagName;
        }
    }

    /**
     * Instruments a method by adding null-check assertions for method parameters.
     *
     * @param ci the ClassInfo object representing the class
     * @param mi the MethodInfo object representing the method
     * @throws ClassFileProcessingFailedException if processing of the class file fails
     */
    private void instrumentMethod(ClassInfo ci, MethodInfo mi) throws ClassFileProcessingFailedException {
        String methodName = mi.name();

        if (mi.isSynthetic() || mi.isAbstract()) {
            LOG.fine(() -> "skipping synthetic method " + methodName);
            return ;
        }

        // special case: for record equals, ignore @NonNull annotations except directly on the method parameter
        // see https://github.com/xzel23/cabe/issues/2
        boolean ignoreNonMethodNonNullAnnotation = ci.isRecord()
                && mi.methodName().equals("equals") && mi.parameters().size() == 1;

        LOG.fine(() -> "instrumenting method " + methodName);
        boolean hasStandardAssertions = false;
        boolean hasOtherChecks = false;
        try (Formatter standardAssertions = new Formatter(); Formatter otherChecks = new Formatter()) {
            // create assertion code
            for (ParameterInfo pi : mi.parameters()) {
                // do not add assertions for synthetic parameters, primitive types and constructors of anonymous classes
                if (!mi.isCanonicalRecordConstructor() && pi.isSynthetic() || pi.type().isPrimitive() || (mi.isConstructor() && ci.isAnonymousClass())) {
                    continue;
                }

                // create assertion code
                NullnessOperator nullnessOperatorParameter = pi.nullnessOperator();
                boolean isNonNull = (nullnessOperatorParameter == NullnessOperator.MINUS_NULL)
                        || (!ignoreNonMethodNonNullAnnotation && ci.nullnessOperator().andThen(nullnessOperatorParameter) == NullnessOperator.MINUS_NULL);
                if (isNonNull) {
                    Configuration.Check check = ci.isPublicApi() && mi.isPublic() ? configuration.publicApi() : configuration.privateApi();
                    if (ci.isRecord() && check== Configuration.Check.ASSERT) { // issue: https://github.com/xzel23/cabe/issues/1
                        LOG.warning("cannot use assert in record " + ci.name() + " / https://github.com/xzel23/cabe/issues/1");
                        LOG.info("using THROW_NPE instead of ASSERT for " + methodName);
                        check = Configuration.Check.THROW_NPE;
                    }
                    switch (check) {
                        case NO_CHECK -> {
                            // nop
                        }
                        case ASSERT -> {
                            standardAssertions.format(
                                    "  if (%1$s==null) { throw new AssertionError((Object) \"%2$s is null\"); }%n",
                                    pi.param(), pi.name()
                            );
                            hasStandardAssertions = true;
                        }
                        case ASSERT_ALWAYS -> {
                            otherChecks.format(
                                    "if (%1$s==null) { throw new AssertionError((Object) \"%2$s is null\"); }%n",
                                    pi.param(), pi.name()
                            );
                            hasOtherChecks = true;
                        }
                        case THROW_NPE -> {
                            otherChecks.format(
                                    "if (%1$s==null) { throw new NullPointerException(\"%2$s is null\"); }%n",
                                    pi.param(), pi.name()
                            );
                            hasOtherChecks = true;
                        }
                    }
                    LOG.fine(() -> "adding null check for parameter " + pi.name() + " in " + ci.name());
                }
            }

            // modify class
            String code = (hasStandardAssertions
                    ? "if (%1$s) {%n%2$s}%n".formatted(getAssertionEnabledExpression(ci), standardAssertions)
                    : "")
                    +
                    (hasOtherChecks ? otherChecks : "");

            if (!code.isEmpty()) {
                LOG.fine(() -> "injecting code into: " + methodName + "\n" + code.indent(2).stripTrailing());
                CtClass ctClass = classPool.getCtClass(ci.name());
                CtBehavior ctBehavior = getCtMethod(ctClass, mi);
                ctBehavior.insertBefore(code);
            }
        } catch (CannotCompileException e) {
            throw new ClassFileProcessingFailedException("compilation failed for instrumented method '" + methodName + "'", e);
        } catch (NotFoundException e) {
            throw new ClassFileProcessingFailedException("class not found while instrumented method '" + methodName + "'", e);
        } catch (RuntimeException e) {
            throw new ClassFileProcessingFailedException("exception while instrumenting method '" + methodName + "'", e);
        }
    }

    /**
     * Retrieves a CtBehavior object representing a method or constructor from the given CtClass that matches the provided MethodInfo.
     *
     * @param ctClass the CtClass object representing the class containing the method or constructor
     * @param mi the MethodInfo object containing information about the method or constructor to retrieve
     * @return the CtBehavior object representing the method or constructor
     * @throws NotFoundException if the method or constructor is not found
     */
    static CtBehavior getCtMethod(CtClass ctClass, MethodInfo mi) throws NotFoundException {
        CtBehavior[] ctBehaviors = mi.isConstructor()
                ? ctClass.getDeclaredConstructors()
                : ctClass.getDeclaredMethods(mi.name());
        return getCtBehavior(mi, ctBehaviors);
    }

    /**
     * Retrieves a {@link CtBehavior} object that matches the parameter types of the given {@link MethodInfo} object.
     *
     * @param mi               the {@link MethodInfo} object representing the method to match
     * @param declaredBehaviors  an array of {@link CtBehavior} objects representing the declared methods/constructors
     * @return the {@link CtBehavior} object that matches the parameter types of the given {@link MethodInfo} object
     * @throws NotFoundException if the method is not found
     */
    static CtBehavior getCtBehavior(MethodInfo mi, CtBehavior[] declaredBehaviors) throws NotFoundException {
        List<String> params = Arrays.stream(mi.method().getParameterTypes()).map(Class::getCanonicalName).toList();
        for (CtBehavior ctBehavior : declaredBehaviors) {
            List<String> ctParams = Arrays.stream(ctBehavior.getParameterTypes()).map(ClassPatcher::getCanonicalClassName).toList();
            if (ctParams.equals(params)) {
                return ctBehavior;
            }
        }
        throw new IllegalStateException("method not found: " + mi);
    }

    /**
     * Retrieves the canonical class name from a given CtClass instance.
     *
     * <p>The method replaces the '$' character in the class name returned for inner classes by Javassist with '.'
     * to be compatible with {@link Class#getCanonicalName()}.
     *
     * @param ctClass the CtClass instance from which to retrieve the canonical class name
     * @return the canonical class name as a String
     */
    private static String getCanonicalClassName(CtClass ctClass) {
        return ctClass.getName().replace('$', '.');
    }

    /**
     * Retrieves the class name from a given class file path.
     *
     * @param classFile the path to the class file
     * @return the class name extracted from the class file path
     */
    private String getClassName(Path classFile) {
        return GET_CLASS_NAME_PATTERN.matcher(inputFolder.relativize(classFile).toString()).replaceFirst("")
                .replace(File.separatorChar, '.');
    }

}
