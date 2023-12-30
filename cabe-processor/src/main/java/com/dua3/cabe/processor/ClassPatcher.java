package com.dua3.cabe.processor;

import com.dua3.cabe.annotations.NotNull;
import com.dua3.cabe.annotations.NotNullApi;
import com.dua3.cabe.annotations.Nullable;
import com.dua3.cabe.annotations.NullableApi;
import com.dua3.utility.options.Arguments;
import com.dua3.utility.options.ArgumentsParser;
import com.dua3.utility.options.ArgumentsParserBuilder;
import com.dua3.utility.options.Flag;
import com.dua3.utility.options.Option;
import com.dua3.utility.options.OptionException;
import com.dua3.utility.options.SimpleOption;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.Descriptor;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

    private static final Pattern PATTERN_FQCN = Pattern.compile("^(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\$\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*$");
    private static final Pattern PATTERN_INNER_CLASS_NAME = Pattern.compile("^([_$a-zA-Z][_$a-zA-Z0-9]*\\.)*[_$a-zA-Z][_$a-zA-Z0-9]*\\$[_$a-zA-Z0-9]*");
    private static final Pattern PATTERN_ANONYMOUS_CLASS_SUFFIX = Pattern.compile(".*\\$\\d+");

    /**
     * This method is the entry point of the application.
     *
     * @param args an array of command-line arguments
     * @throws IOException                        if an I/O error occurs
     * @throws ClassFileProcessingFailedException if processing of a class file fails
     */
    public static void main(String[] args) throws IOException, ClassFileProcessingFailedException {
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

        ArgumentsParserBuilder builder = ArgumentsParser.builder()
                .name("cabe")
                .description("Add null checks in Java class file byte code.")
                .positionalArgs(0, 0);
        SimpleOption<Path> optInput = builder.simpleOption(Path.class, "--input", "-i", "Input folder containing class files").required();
        SimpleOption<Path> optOutput = builder.simpleOption(Path.class, "--output", "-o", "Output folder for patched class files").required();
        Option<Path> optClasspath = builder.option(Path.class, "--classpath", "-c", "Java compile classpath").minArity(0).occurrence(0, 1);
        Flag optHelp = builder.flag("--help", "-h", "Show help");

        ArgumentsParser argsParser = builder.build();

        try {
            Arguments parsedArgs = argsParser.parse(args);

            if (parsedArgs.isSet(optHelp)) {
                argsParser.help();
                return;
            }

            Path in = parsedArgs.getOrThrow(optInput);
            Path out = parsedArgs.getOrThrow(optOutput);
            List<Path> classPaths = parsedArgs.stream(optClasspath).flatMap(List::stream).toList();
            ClassPatcher classPatcher = new ClassPatcher(classPaths);
            classPatcher.processFolder(in, out);
        } catch (OptionException e) {
            System.err.println(e.getMessage());
            System.out.println();
            System.out.println(argsParser.help());
        }
    }

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ClassPatcher.class.getName());
    private static final ParameterInfo[] EMPTY_PARAMETER_INFO = {};
    private static final Set<String> PRIMITIVES = Set.of(
            "byte",
            "char",
            "double",
            "float",
            "int",
            "long",
            "short",
            "boolean"
    );
    private static final Pattern PATTERN_SYNTHETIC_PARAMETER_NAMES = Pattern.compile("this(\\$\\d+)?");
    private static final Pattern GET_CLASS_NAME_PATTERN = Pattern.compile("\\.[^.]*$");

    private final List<Path> classpath;
    private ClassPool pool;
    private Path inputFolder;
    private Path outputFolder;

    /**
     * This class represents a ClassPatcher object that manipulates class files in a given classpath.
     *
     * @param classpath the compile classpath
     */
    public ClassPatcher(Collection<Path> classpath) {
        this.classpath = new ArrayList<>(classpath);
    }

    /**
     * Process a folder containing class files.
     *
     * @param inputFolder the folder to process
     * @param outputFolder the folder to write the patched files to
     * @throws IOException                        if an I/O error occurs
     * @throws ClassFileProcessingFailedException if processing of a class file fails
     */
    public synchronized void processFolder(Path inputFolder, Path outputFolder) throws IOException, ClassFileProcessingFailedException {
        try {
            LOG.fine(() -> "process folder " + inputFolder);

            this.inputFolder = Objects.requireNonNull(inputFolder, "input folder is null");
            this.outputFolder = Objects.requireNonNull(outputFolder, "output folder is null");
            this.pool = new ClassPool(true);

            // no directory
            if (!Files.isDirectory(inputFolder)) {
                LOG.warning("does not exist or is not a directory: " + inputFolder);
                return;
            }

            classpath.forEach(cp -> {
                try {
                    pool.appendClassPath(cp.toString());
                } catch (NotFoundException e) {
                    LOG.warning("could not add to classpath: " + cp);
                }
            });

            try {
                pool.appendClassPath(inputFolder.toString());
            } catch (NotFoundException e) {
                throw new ClassFileProcessingFailedException("could not append classes folder to classpath: " + inputFolder, e);
            }

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
            this.pool = null;
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
     * Checks if a given class element is annotated with the specified annotation.
     *
     * @param el         the class element to check for annotation
     * @param annotation the annotation to check for
     * @return true if the class element is annotated with the specified annotation, false otherwise
     * @throws ClassNotFoundException if the specified annotation class cannot be found
     */
    private static boolean isAnnotated(CtClass el, Class<? extends java.lang.annotation.Annotation> annotation) throws ClassNotFoundException {
        return el.getAnnotation(annotation) != null;
    }

    /**
     * Instruments a class file by adding null-check assertions for method parameters.
     *
     * @param classFile the path to the class file to be instrumented
     * @throws ClassFileProcessingFailedException if processing of the class file fails
     * @throws IOException                        if an I/O error occurs
     */
    private void instrumentClassFile(Path classFile) throws ClassFileProcessingFailedException, IOException {
        LOG.fine(() -> "Instrumenting class file: " + classFile);

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
            CtClass ctClass = pool.get(className);

            try {
                boolean isNotNullApi = isNotNullApi(ctClass);

                for (CtBehavior method : ctClass.getDeclaredBehaviors()) {
                    instrumentMethod(classFile, className, method, isNotNullApi);
                }

                // Write the class file
                LOG.fine("writing modified class file: " + classFile);
                Files.createDirectories(outputFolder.resolve(inputFolder.relativize(classFile.getParent())));
                ctClass.writeFile(outputFolder.toString());

                LOG.fine("instrumenting class file successful: " + classFile);
            } catch (ClassNotFoundException e) {
                throw new ClassFileProcessingFailedException("Failed to modify class file " + classFile, e);
            } finally {
                ctClass.detach();
            }
        } catch (IOException e) {
            throw new IOException("Failed to modify class file " + classFile, e);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "instrumenting class file failed: " + classFile, e);
            throw new ClassFileProcessingFailedException("Failed to modify class file " + classFile, e);
        }
    }

    /**
     * Checks if a given {@link CtClass} is contained in a package annotated with the {@link NotNullApi} annotation.
     *
     * @param ctClass the {@link CtClass} to check for annotation
     * @return true if the {@link CtClass} is contained in a package annotated with the {@link NotNullApi} annotation, false otherwise
     * @throws ClassNotFoundException if the specified annotation class cannot be found
     * @throws IllegalStateException if the package is annotated with both {@link NotNullApi} and {@link NullableApi}
     */
    private boolean isNotNullApi(CtClass ctClass) throws ClassNotFoundException {
        String pkgName = ctClass.getPackageName();

        boolean isNotNullApi = false;
        boolean isNullableApi = false;
        try {
            CtClass pkg = pool.get(pkgName + ".package-info");
            isNotNullApi = isAnnotated(pkg, NotNullApi.class);
            isNullableApi = isAnnotated(pkg, NullableApi.class);
        } catch (NotFoundException e) {
            LOG.fine(() -> "no package-info: " + pkgName);
        }
        LOG.fine("package " + pkgName + " annotations: "
                + (isNotNullApi ? "@" + NotNullApi.class.getSimpleName() : "")
                + (isNullableApi ? "@" + NullableApi.class.getSimpleName() : "")
        );
        if (isNotNullApi && isNullableApi) {
            throw new IllegalStateException(
                    "package " + pkgName + " is annotated with both "
                            + NotNullApi.class.getSimpleName()
                            + " and "
                            + NullableApi.class.getSimpleName()
            );
        }
        return isNotNullApi;
    }

    /**
     * Instruments a method by adding null-check assertions for method parameters.
     *
     * @param classFile    the path to the class file
     * @param className    the name of the class containing the method
     * @param method       the method to be instrumented
     * @param isNotNullApi a flag indicating whether the method is inside a @{@link NotNullApi} annotated package
     * @return true if the method was modified, false otherwise
     * @throws ClassFileProcessingFailedException if processing fails
     */
    private static boolean instrumentMethod(Path classFile, String className, CtBehavior method, boolean isNotNullApi) throws ClassFileProcessingFailedException {
        String methodName = method.getLongName();
        LOG.fine(() -> "instrumenting method " + methodName);

        if (Modifier.isAbstract(method.getModifiers())) {
            LOG.fine(() -> "skipping abstract method");
            return false;
        }

        if (Modifier.isVolatile(method.getModifiers())) {
            LOG.fine(() -> "skipping bridge method");
            return false;
        }

        boolean isChanged = false;
        try (Formatter assertions = new Formatter()) {
            ParameterInfo[] parameterInfo = getParameterInfo(method);
            for (ParameterInfo pi : parameterInfo) {
                // do not add assertions for primitive types and constructors of anonymous classes
                if (PRIMITIVES.contains(pi.type) || (pi.isConstructor && pi.isClassAnonymous)) {
                    continue;
                }

                // consistency check
                if (pi.isNotNullAnnotated && pi.isNullableAnnotated) {
                    throw new IllegalStateException(
                            "parameter " + pi.name + " is annotated with both @NotNull and @Nullable"
                                    + " in method " + methodName
                    );
                }

                // create assertion code
                boolean isNotNull = pi.isNotNullAnnotated || isNotNullApi && !pi.isNullableAnnotated;
                if (isNotNull) {
                    LOG.fine(() -> "adding assertion for parameter " + pi.name + " in " + classFile);
                    assertions.format(
                            "if (%1$s.class.desiredAssertionStatus() && (%2$s==null)) {%n"
                                    + "  throw new AssertionError((Object) \"parameter '%3$s' must not be null\");%n"
                                    + "}%n",
                            className, pi.param, pi.name
                    );
                    isChanged = true;
                }
            }

            // modify class
            if (isChanged) {
                String src = assertions.toString();
                LOG.fine(() -> "injecting code\n  method: " + methodName + "  code:\n" + src.indent(2));
                method.insertBefore(src);
            }

            return isChanged;
        } catch (CannotCompileException e) {
            throw new ClassFileProcessingFailedException("compilation failed for method '" + methodName + "'", e);
        } catch (ClassNotFoundException e) {
            throw new ClassFileProcessingFailedException("class not found while instrumenting method '" + methodName + "'", e);
        } catch (RuntimeException | NotFoundException e) {
            throw new ClassFileProcessingFailedException("exception while instrumenting method '" + methodName + "'", e);
        }
    }

    /**
     * Class representing information about a method parameter.
     */
    public record ParameterInfo(String param, String name, String type,
                                boolean isNotNullAnnotated, boolean isNullableAnnotated,
                                boolean isClassStatic, boolean isClassInner, boolean isClassAnonymous, boolean isMethod,
                                boolean isConstructor, boolean isInterface, boolean isAbstract) {

        @Override
        public String toString() {
            return param + ":"
                    + (isNotNullAnnotated ? " @NotNull" : "")
                    + (isNullableAnnotated ? " @Nullable" : "")
                    + " " + type
                    + " " + name;
        }
    }

    /**
     * Retrieves information about the parameters of a given method.
     *
     * @param method the method to retrieve parameter information for
     * @return an array of ParameterInfo objects representing the parameters of the method
     * @throws ClassNotFoundException if the method parameter types cannot be found
     * @throws NotFoundException if the method parameter types cannot be found
     */
    public static ParameterInfo[] getParameterInfo(CtBehavior method) throws ClassNotFoundException, NotFoundException {
        String methodName = method.getLongName();
        LOG.fine("collecting parameter information for " + methodName);

        MethodInfo methodInfo = method.getMethodInfo();
        if (methodInfo == null) {
            throw new IllegalStateException("could not get method info for method " + methodName);
        }

        CtClass declaringClass = method.getDeclaringClass();
        String declaringClassName = declaringClass.getName();

        boolean isInnerClass = PATTERN_INNER_CLASS_NAME.matcher(declaringClassName).matches();
        boolean isStaticClass = Modifier.isStatic(declaringClass.getModifiers());
        boolean isAnonymousInnerClass = isInnerClass && !isStaticClass && PATTERN_ANONYMOUS_CLASS_SUFFIX.matcher(declaringClassName).matches();

        boolean isConstructor = methodInfo.isConstructor();
        boolean isMethod = methodInfo.isMethod();
        boolean isInterface = declaringClass.isInterface();
        boolean isAbstract = Modifier.isAbstract(method.getModifiers());

        if (!isConstructor && !isMethod || isConstructor && isInterface || isAbstract) {
            return EMPTY_PARAMETER_INFO;
        }

        // read parameter annotations and types
        Object[][] parameterAnnotations = method.getParameterAnnotations();
        String[] types = getParameterTypes(method);

        // determine the actual number of method parameters
        boolean isParentTypePassed = isConstructor && isInnerClass && !isStaticClass;
        boolean isEnumConstructor = declaringClass.isEnum() && isConstructor;

        int parameterCount = types.length;
        int typeOffset = 0;
        if (isParentTypePassed) {
            parameterCount--; // this is passed implicitly
        }
        if (isEnumConstructor) {
            // enum constructors are called with two additional synthetic arguments (name ant ordinal)
            parameterCount -= 2;
            typeOffset += 2;
        }
        int firstParameterNumber = 1 + types.length - parameterCount;

        // fastpath if no parameters
        if (parameterCount < 1) {
            return EMPTY_PARAMETER_INFO;
        }

        // determine the number of synthetic arguments (i.e. 'this' of parent classes for inner classes)
        CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
        if (codeAttribute == null) {
            throw new IllegalStateException("code attribute is null for method " + methodName);
        }
        if (!(codeAttribute.getAttribute(LocalVariableAttribute.tag) instanceof LocalVariableAttribute lva)) {
            throw new IllegalStateException("could not get local variable info for method " + methodName);
        }

        int lvaLength = lva.tableLength();
        int syntheticArgsCount = 0;
        while ((syntheticArgsCount < lvaLength && PATTERN_SYNTHETIC_PARAMETER_NAMES.matcher(getParameterName(lva, syntheticArgsCount, 0)).matches())) {
            syntheticArgsCount++;
        }

        if (isConstructor && isAnonymousInnerClass && syntheticArgsCount >= lvaLength) {
            return EMPTY_PARAMETER_INFO;
        }

        // create the return array
        ParameterInfo[] parameterInfo = new ParameterInfo[parameterCount];

        for (int i = 0; i < parameterCount; i++) {
            String name = getParameterName(lva, syntheticArgsCount, i);
            boolean isNotNullAnnotated = false;
            boolean isNullableAnnotated = false;
            for (Object annotation : parameterAnnotations[i]) {
                isNotNullAnnotated = isNotNullAnnotated || (annotation instanceof NotNull);
                isNullableAnnotated = isNullableAnnotated || (annotation instanceof Nullable);
            }
            String type = types[typeOffset + i];
            String param = "$" + (firstParameterNumber + i);
            parameterInfo[i] = new ParameterInfo(param, name, type, isNotNullAnnotated, isNullableAnnotated,
                    isStaticClass, isInnerClass, isAnonymousInnerClass,
                    isMethod, isConstructor, isInterface, isAbstract);
        }

        LOG.finest(() -> methodName + ": " + Arrays.toString(parameterInfo));

        return parameterInfo;
    }

    private static String getParameterName(LocalVariableAttribute lva, int syntheticArgsCount, int i) {
        int idx = syntheticArgsCount + i;
        for (int j = 0; j < lva.tableLength(); j++) {
            if (lva.index(j) == idx) {
                return lva.variableName(j);
            }
        }
        if (idx < lva.tableLength()) {
            return lva.variableName(idx);
        }
        return "[parameter " + (i + 1) + "]";
    }

    /**
     * Retrieves the parameter types of a given method.
     *
     * @param method the method to retrieve parameter types for
     * @return an array of Strings representing the parameter types of the method
     * @throws IllegalStateException if the parameter descriptor is malformed
     */
    public static String[] getParameterTypes(CtBehavior method) {
        String descriptor = method.getSignature();
        String paramsDesc = Descriptor.getParamDescriptor(descriptor);

        if (paramsDesc.length() < 2) {
            throw new IllegalStateException("parameter descriptor length expected to be at least 2: \"" + paramsDesc + "\" for method " + method.getLongName());
        }
        if (paramsDesc.charAt(0) != '(') {
            throw new IllegalStateException("'(' expected at the beginning of parameter descriptor: \"" + paramsDesc + "\" for method " + method.getLongName());
        }
        if (paramsDesc.charAt(paramsDesc.length() - 1) != ')') {
            throw new IllegalStateException("'(' expected at the end of parameter descriptor: ': \"" + paramsDesc + "\" for method " + method.getLongName());
        }

        ArrayList<String> params = new ArrayList<>();
        for (int i = 1; i < paramsDesc.length() - 1; ) {
            StringBuilder type = new StringBuilder();

            while (paramsDesc.charAt(i) == '[') {
                type.append("[]");
                i++;
            }

            char c = paramsDesc.charAt(i);
            switch (c) {
                case 'B':
                    type.insert(0, "byte");
                    break;
                case 'C':
                    type.insert(0, "char");
                    break;
                case 'D':
                    type.insert(0, "double");
                    break;
                case 'F':
                    type.insert(0, "float");
                    break;
                case 'I':
                    type.insert(0, "int");
                    break;
                case 'J':
                    type.insert(0, "long");
                    break;
                case 'S':
                    type.insert(0, "short");
                    break;
                case 'Z':
                    type.insert(0, "boolean");
                    break;
                case 'L':
                    int endIndex = paramsDesc.indexOf(';', i);
                    // Get the text between 'L' and ';', replace '/' with '.'.
                    String className = paramsDesc.substring(i + 1, endIndex).replace('/', '.');
                    type.insert(0, className);
                    i = endIndex;
                    break;
                default:
                    throw new IllegalStateException("invalid character in parameter descriptor: '" + c + "'");
            }
            params.add(type.toString());
            i++;
        }

        return params.toArray(new String[0]);
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
