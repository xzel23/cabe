package com.dua3.cabe.processor;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents information about a method.
 */
record MethodInfo(String name, boolean isConstructor, boolean isCanonicalRecordConstructor, boolean isMethod,
                  boolean isAbstract, boolean isStatic,
                  boolean isPublic, boolean isSynthetic, boolean isBridge, boolean isNative,
                  List<ParameterInfo> parameters, ClassInfo classInfo, CtBehavior ctMethod) {
    private static final Pattern PATTERN_EXTRACT_METHOD_NAME = Pattern.compile(".*\\.([\\w$]+)\\(.*");

    public static MethodInfo forMethod(ClassInfo ci, CtBehavior ctMethod) {
        var methodInfo = ctMethod.getMethodInfo();
        boolean isConstructor = methodInfo.isConstructor();
        boolean isCanonicalRecordConstructor = isCanonicalRecordConstructor(ci, ctMethod);
        boolean isMethod = methodInfo.isMethod();
        boolean isAbstract = Modifier.isAbstract(ctMethod.getModifiers());
        boolean isStatic = Modifier.isStatic(ctMethod.getModifiers());
        boolean isPublicApi = ci.isPublicApi() && Modifier.isPublic(ctMethod.getModifiers());

        int accessFlags = ctMethod.getMethodInfo().getAccessFlags();
        boolean isSynthetic = (accessFlags & AccessFlag.SYNTHETIC) != 0;
        boolean isBridge = (accessFlags & AccessFlag.BRIDGE) != 0;
        boolean isNative = (accessFlags & AccessFlag.NATIVE) != 0;

        List<ParameterInfo> parameters = new ArrayList<>();

        MethodInfo mi = new MethodInfo(
                ctMethod.getLongName(),
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
                ctMethod);

        parameters.addAll(ParameterInfo.forMethod(mi));

        return mi;
    }

    private static boolean isCanonicalRecordConstructor(ClassInfo ci, CtBehavior method) {
        // is it a record constructor?
        if (!ci.isRecord() || !(method instanceof CtConstructor constructor)) {
            return false;
        }

        try {
            CtClass declaringClass = constructor.getDeclaringClass();
            CtClass[] parameterTypes = constructor.getParameterTypes();

            // constructor arguments must match record components
            if (parameterTypes.length != declaringClass.getDeclaredFields().length) {
                return false;
            }
            for (int i = 0; i < parameterTypes.length; i++) {
                CtField field = declaringClass.getDeclaredFields()[i];
                if (!field.getType().equals(parameterTypes[i])) {
                    return false;
                }
            }

            // it is the canonical constructor
            return true;
        } catch (NotFoundException e) {
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
                ", isConstructor=" + isConstructor +
                ", isMethod=" + isMethod +
                ", isAbstract=" + isAbstract +
                ", isStatic=" + isStatic +
                ", isSynthetic=" + isSynthetic +
                ", isBridge=" + isBridge +
                ", isNative=" + isNative +
                ", parameters=" + parameters +
                ", classInfo=" + classInfo.name() +
                ", ctMethod=" + ctMethod +
                '}';
    }
}
