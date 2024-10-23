package com.dua3.cabe.processor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents information about a method.
 */
record MethodInfo(String name, String fullMethodName, boolean isConstructor, boolean isCanonicalRecordConstructor, boolean isMethod,
                  boolean isAbstract, boolean isStatic,
                  boolean isPublic, boolean isSynthetic, boolean isBridge, boolean isNative,
                  List<ParameterInfo> parameters, ClassInfo classInfo,
                  Executable method) {
    private static final Pattern PATTERN_EXTRACT_METHOD_NAME = Pattern.compile(".*\\.([\\w$]+)\\(.*");

    public static MethodInfo forMethod(ClassInfo ci, Executable method) {
        int modifiers = method.getModifiers();

        boolean isConstructor = method instanceof Constructor<?>;
        boolean isCanonicalRecordConstructor = isCanonicalRecordConstructor(ci, method);
        boolean isMethod = method instanceof Method m;
        boolean isAbstract = Modifier.isAbstract(modifiers);
        boolean isStatic = Modifier.isStatic(modifiers);
        boolean isPublicApi = ci.isPublicApi() && Modifier.isPublic(modifiers);

        boolean isSynthetic = method.isSynthetic();
        boolean isBridge = isMethod && ((Method) method).isBridge();
        boolean isNative = Modifier.isNative(modifiers);

        List<ParameterInfo> parameters = new ArrayList<>();

        MethodInfo mi = new MethodInfo(
                method.getName(),
                method.toGenericString(),
                isConstructor,
                isCanonicalRecordConstructor,
                isMethod,
                isAbstract,
                isStatic,
                isPublicApi,
                isSynthetic,
                isBridge,
                isNative,
                parameters,
                ci,
                method);

        parameters.addAll(ParameterInfo.forMethod(mi));

        return mi;
    }

    private static boolean isCanonicalRecordConstructor(ClassInfo ci, Executable method) {
        // is it a record constructor?
        if (!ci.isRecord() || !(method  instanceof Constructor<?>)) {
            return false;
        }

        try {
            Class<?> declaringClass = method.getDeclaringClass();
            Parameter[] parameterTypes = method.getParameters();

            // constructor arguments must match record components
            if (parameterTypes.length != declaringClass.getDeclaredFields().length) {
                return false;
            }
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> fieldType = declaringClass.getDeclaredFields()[i].getType();
                if (!fieldType.equals(parameterTypes[i].getType())) {
                    return false;
                }
            }

            // it is the canonical constructor
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<ParameterInfo> parameters() {
        return Collections.unmodifiableList(parameters);
    }

    public String methodName() {
        return PATTERN_EXTRACT_METHOD_NAME.matcher(name()).replaceFirst("$1");
    }

    @Override
    public String toString() {
        return "MethodInfo{" +
                "name='" + name + '\'' +
                ", fullMethodName='" + fullMethodName + '\'' +
                ", isConstructor=" + isConstructor +
                ", isMethod=" + isMethod +
                ", isAbstract=" + isAbstract +
                ", isStatic=" + isStatic +
                ", isSynthetic=" + isSynthetic +
                ", isBridge=" + isBridge +
                ", isNative=" + isNative +
                ", parameters=" + parameters +
                ", classInfo=" + classInfo.name() +
                ", method=" + method +
                '}';
    }
}
