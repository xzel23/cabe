package com.dua3.cabe.processor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The ClassInfo class represents information about a Java class.
 */
record ClassInfo(String name, boolean isInnerClass, boolean isStaticClass, boolean isInterface, boolean isEnum,
                 boolean isRecord, boolean isDerived, boolean isAnonymousClass, boolean isPublicApi,
                 NullnessOperator nullnessOperator,
                 String assertionsDisabledFlagName, List<MethodInfo> methods, Class<?> cls) {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ClassInfo.class.getName());
    private static final Pattern PATTERN_INNER_CLASS_NAME = Pattern.compile("^([_$a-zA-Z][_$a-zA-Z0-9]*\\.)*[_$a-zA-Z][_$a-zA-Z0-9]*\\$[_$a-zA-z0-9]*");
    private static final Pattern PATTERN_ANONYMOUS_CLASS_SUFFIX = Pattern.compile(".*\\$\\d+");

    @Override
    public List<MethodInfo> methods() {
        return Collections.unmodifiableList(methods);
    }

    /**
     * Generates a {@link ClassInfo} object for the specified class.
     *
     * @param cls the class
     * @return a {@link ClassInfo} object representing the specified class
     */
    public static ClassInfo forClass(Class<?> cls) {
        String className = cls.getName();

        int modifiers = cls.getModifiers();
        boolean isInnerClass = PATTERN_INNER_CLASS_NAME.matcher(className).matches();
        boolean isStaticClass = Modifier.isStatic(modifiers);
        boolean isAnonymousClass = isInnerClass && !isStaticClass && PATTERN_ANONYMOUS_CLASS_SUFFIX.matcher(className).matches();
        boolean isInterface = Modifier.isInterface(modifiers);
        boolean isEnum = cls.isEnum();
        boolean isRecord = cls.isRecord();
        boolean isDerived = cls.getSuperclass() != null && !cls.getSuperclass().getName().equals(Object.class.getName()) && !isEnum && !isRecord;
        NullnessOperator nullnessOperator = Util.getClassNullnessOperator(cls);
        boolean isPublicApi = Modifier.isPublic(modifiers) || Util.hasPublicApiAncestor(cls);
        String assertionsDisabledFlagName = Util.getAssertionsDisabledFlagName(cls);

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
                isPublicApi,
                nullnessOperator,
                assertionsDisabledFlagName,
                methods,
                cls);

        Arrays.stream(cls.getDeclaredConstructors())
                .filter(m -> !m.isSynthetic())
                .map(m -> MethodInfo.forMethod(ci, m))
                .forEach(methods::add);

        Arrays.stream(cls.getDeclaredMethods())
                .filter(m -> !m.isSynthetic() && !m.isBridge())
                .sorted(Comparator.comparing(Method::getName))
                .map(m -> MethodInfo.forMethod(ci, m))
                .forEach(methods::add);

        return ci;
    }

}
