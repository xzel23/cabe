package com.dua3.cabe.processor;

import com.dua3.cabe.annotations.NotNull;
import com.dua3.cabe.annotations.NotNullApi;
import com.dua3.cabe.annotations.Nullable;
import com.dua3.cabe.annotations.NullableApi;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ConstPool;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The ClassPatcher class is responsible for patching class files by adding assertions for nullability checks
 * on method parameters. It collects information about the class file, including package information and
 * parameter annotations, and modifies the class file by injecting the necessary code.
 */
public class ClassPatcher {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ClassPatcher.class.getName());
    private static final ParameterInfo[] EMPTY_PARAMETER_INFO = {};

    private ClassPool pool = ClassPool.getDefault();
    private Path classFolder;

    public ClassPatcher() {
    }

    /**
     * Process a folder containing class files.
     *
     * @param classFolder the folder to process
     * @throws IOException                         if an I/O error occurs
     * @throws ClassFileProcessingFailedException if processing of a class file fails
     */
    public synchronized void processFolder(Path classFolder) throws IOException, ClassFileProcessingFailedException {
        try {
            LOG.fine(() -> "process folder " + classFolder);

            this.classFolder = classFolder;

            // no class folder.
            Objects.requireNonNull(classFolder, "folder is null");

            // no directory
            if (!Files.isDirectory(classFolder)) {
                LOG.warning("Does not exist or is not a directory: " + classFolder);
                return;
            }

            List<Path> classFiles;
            try (Stream<Path> paths = Files.walk(classFolder)) {
                classFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(f -> f.getFileName().toString().endsWith(".class"))
                        .filter(f -> !f.getFileName().toString().equals("module-info.class"))
                        .collect(Collectors.toList());
            }

            if (classFiles.isEmpty()) {
                LOG.info("no class files!");
                return;
            }

            ClassPath classPath = null;
            try {
                classPath = pool.appendClassPath(classFolder.toString());
                processClassFiles(classFiles);
            } catch (NotFoundException e) {
                throw new IllegalStateException("could not append path to classpath: " + classFolder, e);
            } finally {
                if (classPath != null) {
                    pool.removeClassPath(classPath);
                }
            }
        } finally {
            this.pool = null;
            this.classFolder = null;
        }
    }

    /**
     * Processes a list of class files by calling the {@link #instrumentClassFile(Path)} method for each file.
     *
     * @param classFiles the list of class files to process
     * @throws IOException                         if an I/O error occurs
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
     * @param el the class element to check for annotation
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
     * @throws IOException if an I/O error occurs
     */
    public void instrumentClassFile(Path classFile) throws ClassFileProcessingFailedException, IOException {
        LOG.info(() -> "Instrumenting class file: " + classFile);

        try {
            String className = getClassName(classFile);
            CtClass ctClass = pool.get(className);

            try {
                String pkgName = ctClass.getPackageName();

                boolean isNotNullApi = false;
                boolean isNullableApi = false;
                try {
                    CtClass pkg = pool.get(pkgName + ".package-info");
                    isNotNullApi = isAnnotated(pkg, NotNullApi.class);
                    isNullableApi = isAnnotated(pkg, NullableApi.class);
                } catch (NotFoundException e) {
                    LOG.warning("no package-info: " + pkgName);
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

                for (CtBehavior method : ctClass.getDeclaredBehaviors()) {
                    String methodName = method.getLongName();
                    LOG.fine(() -> "instrumenting method " + methodName);

                    List<String> assertions = new ArrayList<>();
                    ParameterInfo[] parameterInfo = getParameterInfo(method);
                    for (ParameterInfo pi : parameterInfo) {
                        // do not add assertions for primitive types
                        if (pi.type.isPrimitive()) {
                            continue;
                        }

                        // consistency check
                        if (pi.isNotNullAnnotated && pi.isNullableAnnotated) {
                            throw new IllegalStateException(
                                    "Parameter " + pi.name + " is annotated with both @NotNull and @Nullable"
                            );
                        }

                        boolean isNotNull = pi.isNotNullAnnotated || isNotNullApi && !pi.isNullableAnnotated;
                        if (isNotNull) {
                            LOG.fine(() -> "adding assertion for parameter " + pi.name + " in " + classFile);
                            assertions.add(
                                    "if (" + className + ".class.desiredAssertionStatus() && (" + pi.param + "==null)) {\n" +
                                            "  throw new AssertionError((Object) \"parameter '" + pi.name + "' must not be null\");\n" +
                                            "}\n"
                            );
                        }
                    }
                    for (int i = assertions.size() - 1; i >= 0; i--) {
                        String src = assertions.get(i);
                        LOG.fine(() -> "injecting code\n  method: " + methodName + "  code:\n  " + src.replaceAll("\n", "\n  "));
                        method.insertBefore(src);
                    }
                }

                // Write the changes back to the class file
                LOG.fine("writing modified class file: " + classFile);
                ctClass.writeFile(classFolder.toString());
                LOG.fine("instrumenting class file successful: " + classFile);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } finally {
                ctClass.detach();
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "instrumenting class file failed: " + classFile, e);
            throw new ClassFileProcessingFailedException("Failed to modify class file " + classFile, e);
        }
    }

    /**
     * Class representing information about a method parameter.
     */
    private static class ParameterInfo {
        final String name;
        final String param;
        final CtClass type;
        final boolean isNotNullAnnotated;
        final boolean isNullableAnnotated;

        ParameterInfo(String name, String param, CtClass type, boolean isNotNullAnnotated, boolean isNullableAnnotated) {
            this.name = name;
            this.param = param;
            this.type = type;
            this.isNotNullAnnotated = isNotNullAnnotated;
            this.isNullableAnnotated = isNullableAnnotated;
        }
    }

    /**
     * Retrieves information about the parameters of a given method.
     *
     * @param method the method to retrieve parameter information for
     * @return an array of ParameterInfo objects representing the parameters of the method
     * @throws ClassNotFoundException if the method parameter types cannot be found
     * @throws NotFoundException if the method information cannot be found
     */
    private static ParameterInfo[] getParameterInfo(CtBehavior method) throws ClassNotFoundException, NotFoundException {
        String methodName = method.getLongName();
        LOG.fine("collecting parameter information for " + methodName);

        MethodInfo methodInfo = method.getMethodInfo();
        if (methodInfo == null) {
            throw new IllegalStateException("could not get method info");
        }

        if (!methodInfo.isConstructor() && !methodInfo.isMethod()) {
            return EMPTY_PARAMETER_INFO;
        }

        CtClass declaringClass = method.getDeclaringClass();

        // read parameter annotations and types
        Object[][] parameterAnnotations = method.getParameterAnnotations();
        CtClass[] types = method.getParameterTypes();

        // determine actual number of method parameters
        boolean isThisPassedAsArgument = !Modifier.isStatic(method.getModifiers()) && !methodInfo.isConstructor();
        boolean isParentPassedAsType = methodInfo.isConstructor() && !Modifier.isStatic(declaringClass.getModifiers());
        boolean isEnumConstructor = declaringClass.isEnum() && methodInfo.isConstructor();

        int parameterCount = types.length;;
        int typeOffset = 0;
        if (isParentPassedAsType) {
            parameterCount--;
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
        AttributeInfo attribute = methodInfo.getCodeAttribute().getAttribute(LocalVariableAttribute.tag);
        if (!(attribute instanceof LocalVariableAttribute)) {
            throw new IllegalStateException("could not get local variable info");
        }
        LocalVariableAttribute lva = (LocalVariableAttribute) attribute;
        ConstPool constPool = methodInfo.getConstPool();
        int syntheticArgumentCount = 0;
        while (constPool.getUtf8Info(lva.nameIndex(syntheticArgumentCount)).matches("this(\\$\\d+)?")) {
            syntheticArgumentCount++;
        }

        // create the return array
        ParameterInfo[] parameterInfo = new ParameterInfo[parameterCount];

        for (int i = 0; i < parameterCount; i++) {
            String name = constPool.getUtf8Info(lva.nameIndex(i + syntheticArgumentCount));
            boolean isNotNullAnnotated = false;
            boolean isNullableAnnotated = false;
            for (Object annotation : parameterAnnotations[i]) {
                isNotNullAnnotated = isNotNullAnnotated || (annotation instanceof NotNull);
                isNullableAnnotated = isNullableAnnotated || (annotation instanceof Nullable);
            }
            CtClass type = types[typeOffset + i];
            String param = "$" + (firstParameterNumber + i);
            parameterInfo[i] = new ParameterInfo(name, param, type, isNotNullAnnotated, isNullableAnnotated);
        }
        return parameterInfo;
    }

    /**
     * Retrieves the class name from a given class file path.
     *
     * @param classFile the path to the class file
     * @return the class name extracted from the class file path
     */
    private String getClassName(Path classFile) {
        return classFolder.relativize(classFile).toString()
                .replaceFirst("\\.[^.]*$", "")
                .replace(File.separatorChar, '.');
    }
}
