package com.dua3.cabe.processor;

import com.dua3.cabe.annotations.NotNullApi;
import com.dua3.cabe.annotations.NullableApi;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.Modifier;
import javassist.NotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The ClassInfo class represents information about a Java class.
 */
record ClassInfo(String name, boolean isInnerClass, boolean isStaticClass, boolean isInterface, boolean isEnum,
                 boolean isRecord, boolean isDerived, boolean isAnonymousClass, boolean isNotNullApi, List<MethodInfo> methods,
                 CtClass ctClass) {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ClassInfo.class.getName());
    private static final Pattern PATTERN_INNER_CLASS_NAME = Pattern.compile("^([_$a-zA-Z][_$a-zA-Z0-9]*\\.)*[_$a-zA-Z][_$a-zA-Z0-9]*\\$[_$a-zA-Z0-9]*");
    private static final Pattern PATTERN_ANONYMOUS_CLASS_SUFFIX = Pattern.compile(".*\\$\\d+");

    @Override
    public List<MethodInfo> methods() {
        return Collections.unmodifiableList(methods);
    }

    /**
     * Generates a {@link ClassInfo} object for the specified class.
     *
     * @param classPool the {@link ClassPool} instance to use
     * @param className the fully qualified name of the class
     * @return a {@link ClassInfo} object representing the specified class
     * @throws ClassNotFoundException if the class with the specified name cannot be found
     * @throws NotFoundException      if the class with the specified name is not found in the {@link ClassPool}
     */
    public static ClassInfo forClass(ClassPool classPool, String className) throws ClassNotFoundException, NotFoundException {
        CtClass ctClass = classPool.getCtClass(className);

        int modifiers = ctClass.getModifiers();
        boolean isInnerClass = PATTERN_INNER_CLASS_NAME.matcher(className).matches();
        boolean isStaticClass = Modifier.isStatic(modifiers);
        boolean isAnonymousClass = isInnerClass && !isStaticClass && PATTERN_ANONYMOUS_CLASS_SUFFIX.matcher(className).matches();
        boolean isInterface = Modifier.isInterface(modifiers);
        boolean isEnum = ctClass.isEnum();
        boolean isRecord = ctClass.getSuperclass().getName().equals(Record.class.getName());
        boolean isDerived = !ctClass.getSuperclass().getName().equals(Object.class.getName()) && !isEnum && !isRecord;
        boolean isNotNullApi = isNotNullApi(classPool, ctClass.getPackageName());

        List<MethodInfo> methods = new ArrayList<>();

        ClassInfo ci = new ClassInfo(
                className,
                isInnerClass,
                isStaticClass,
                isInterface,
                isEnum,
                isRecord,
                isDerived,
                isAnonymousClass,
                isNotNullApi,
                methods,
                ctClass);

        Arrays.stream(ctClass.getDeclaredBehaviors())
                .map(m -> MethodInfo.forMethod(ci, m))
                .forEach(methods::add);

        return ci;
    }

    /**
     * Checks if a given {@link CtClass} is contained in a package annotated with the {@link NotNullApi} annotation.
     *
     * @param pkgName the name of the package to check for annotation
     * @return true if the package is contained in a package annotated with the {@link NotNullApi} annotation, false otherwise
     * @throws ClassNotFoundException if the specified annotation class cannot be found
     * @throws IllegalStateException  if the package is annotated with both {@link NotNullApi} and {@link NullableApi}
     */
    private static boolean isNotNullApi(ClassPool classPool, String pkgName) throws ClassNotFoundException {
        boolean isNotNullApi = false;
        boolean isNullableApi = false;
        try {
            CtClass pkg = classPool.get(pkgName + ".package-info");
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
}
