package com.dua3.cabe.processor;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    private static final Pattern PATTERN_FQCN = Pattern.compile("^(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\$\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*$");

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

        Path in = null;
        Path out = null;
        List<Path> classPaths = null;

        try {
            List<String> cmdLine = List.of(args);
            if (cmdLine.contains("--help")) {
                help();
                return;
            }

            if (args.length < 4) {
                throw new IllegalAccessError("Wrong number of arguments");
            }

            int idxInput = cmdLine.indexOf("-i");
            String inputFolder = switch (idxInput) {
                case 0, 2 -> cmdLine.get(idxInput + 1);
                default -> throw new IllegalArgumentException("option '-i' not found at expected position");
            };

            int idxOutput = cmdLine.indexOf("-o");
            String outputFolder = switch (idxOutput) {
                case 0, 2 -> cmdLine.get(idxOutput + 1);
                default -> throw new IllegalArgumentException("option '-o' not found at expected position");
            };

            int idxClasspath = cmdLine.indexOf("-c");
            List<String> classPathFolder = switch (idxClasspath) {
                case 4 ->
                        cmdLine.size() == 5 ? Collections.emptyList() : cmdLine.subList(idxClasspath + 1, cmdLine.size());
                case -1 -> Collections.emptyList();
                default -> throw new IllegalArgumentException("option '-c' not found at expected position");
            };

            in = Paths.get(inputFolder);
            out = Paths.get(outputFolder);
            classPaths = classPathFolder.stream().map(Paths::get).toList();
        } catch (Exception e) {
            System.err.println("Commandline error: " + e.getMessage());
            help();
            System.exit(1);
        }

        assert (in != null && out != null && classPaths != null);

        try {
            ClassPatcher classPatcher = new ClassPatcher(classPaths);
            classPatcher.processFolder(in, out);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
        }
    }

    private static void help() {
        String msg = """
                ClassPatcher
                ============
                                
                Add null checks in Java class file byte code.
                                
                Usage: java -jar <jar-file> -i <input-folder> -o <output-folder> [-c <classpath entry> ...]
                """;
        System.out.println(msg);
    }

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ClassPatcher.class.getName());
    private static final Pattern GET_CLASS_NAME_PATTERN = Pattern.compile("\\.[^.]*$");

    private final List<Path> classpath;
    private ClassPool classPool;
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

            classpath.forEach(cp -> {
                try {
                    classPool.appendClassPath(cp.toString());
                } catch (NotFoundException e) {
                    LOG.warning("could not add to classpath: " + cp);
                }
            });

            try {
                classPool.appendClassPath(inputFolder.toString());
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
            ClassInfo classInfo = ClassInfo.forClass(classPool, className);

            for (var methodInfo : classInfo.methods()) {
                instrumentMethod(classInfo, methodInfo);
                classInfo.ctClass().defrost();
            }

            // Write the class file
            LOG.fine("writing modified class file: " + classFile);
            Files.createDirectories(outputFolder.resolve(inputFolder.relativize(classFile.getParent())));
            classInfo.ctClass().writeFile(outputFolder.toString());

            LOG.fine("instrumenting class file successful: " + classFile);
        } catch (IOException e) {
            throw new IOException("Failed to modify class file " + classFile, e);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "instrumenting class file failed: " + classFile, e);
            throw new ClassFileProcessingFailedException("Failed to modify class file " + classFile, e);
        }
    }

    private static boolean instrumentMethod(ClassInfo ci, MethodInfo mi) throws ClassFileProcessingFailedException {
        String methodName = mi.name();

        if (mi.isSynthetic()) {
            LOG.fine(() -> "skipping synthetic method " + methodName);
            return false;
        }

        LOG.fine(() -> "instrumenting method " + methodName);

        // the default Record.equals() should really be marked as synthetic
        boolean overRideNotNullApi = ci.isRecord() && mi.name().endsWith(".equals(java.lang.Object)");

        boolean isChanged = false;
        try (Formatter assertions = new Formatter()) {
            assertions.format("if (%1$s.class.desiredAssertionStatus()) {%n", ci.name());
            for (ParameterInfo pi : mi.parameters()) {
                // do not add assertions for synthetic parameters, primitive types and constructors of anonymous classes
                if (pi.isSynthetic() || ParameterInfo.isPrimitive(pi.type()) || (mi.isConstructor() && ci.isAnonymousClass())) {
                    continue;
                }

                // create assertion code
                boolean isNotNull = pi.isNotNullAnnotated() || !pi.isNullableAnnotated() && !overRideNotNullApi && ci.isNotNullApi();
                if (isNotNull) {
                    LOG.fine(() -> "adding assertion for parameter " + pi.name() + " in " + ci.name());
                    assertions.format(
                            "  if (%2$s==null) {%n"
                                    + "    throw new AssertionError((Object) \"parameter '%3$s' must not be null\");%n"
                                    + "  }%n",
                            ci.name(), pi.param(), pi.name()
                    );
                    isChanged = true;
                }
            }
            assertions.format("}%n");

            // modify class
            if (isChanged) {
                String src = assertions.toString();
                LOG.fine(() -> "injecting code\n  method: " + methodName + "  code:\n" + src.indent(2));
                mi.ctMethod().insertBefore(src);
            }

            return isChanged;
        } catch (CannotCompileException e) {
            throw new ClassFileProcessingFailedException("compilation failed for method '" + methodName + "'", e);
        } catch (RuntimeException e) {
            throw new ClassFileProcessingFailedException("exception while instrumenting method '" + methodName + "'", e);
        }
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
