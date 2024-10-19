package com.dua3.cabe.processor;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;

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
                  MethodDescription.InDefinedShape methodDescription) {
    private static final Pattern PATTERN_EXTRACT_METHOD_NAME = Pattern.compile(".*\\.([\\w$]+)\\(.*");

    public static MethodInfo forMethod(ClassInfo ci, MethodDescription.InDefinedShape methodDescription) {
        boolean isConstructor = methodDescription.isConstructor();
        boolean isCanonicalRecordConstructor = isCanonicalRecordConstructor(ci, methodDescription);
        boolean isMethod = methodDescription.isMethod();
        boolean isAbstract = methodDescription.isAbstract();
        boolean isStatic = methodDescription.isStatic();
        boolean isPublicApi = ci.isPublicApi() && methodDescription.isPublic();

        boolean isSynthetic = methodDescription.isSynthetic();
        boolean isBridge = methodDescription.isBridge();
        boolean isNative = methodDescription.isNative();

        List<ParameterInfo> parameters = new ArrayList<>();

        MethodInfo mi = new MethodInfo(
                methodDescription.getInternalName(),
                getFullMethodName(methodDescription),
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
                methodDescription);

        parameters.addAll(ParameterInfo.forMethod(mi));

        return mi;
    }

    private static String getFullMethodName(MethodDescription.InDefinedShape methodDescription) {
        StringBuilder sb = new StringBuilder();
        sb.append(methodDescription.getDeclaringType().asErasure().getActualName());
        if (!methodDescription.isConstructor()) {
            sb.append('.').append(methodDescription.getActualName());
        }
        sb.append('(');
        String separator = "";
        for (ParameterDescription.InDefinedShape param : methodDescription.getParameters()) {
            sb.append(separator);
            sb.append(param.getType().asErasure().getActualName());
            separator = ",";
        }
        sb.append(')');
        return sb.toString();
    }

    private static boolean isCanonicalRecordConstructor(ClassInfo ci, MethodDescription method) {
        // is it a record constructor?
        if (!ci.isRecord() || !(method instanceof MethodDescription.InDefinedShape)) {
            return false;
        }

        try {
            TypeDescription declaringClass = method.getDeclaringType().asErasure();
            ParameterList<?> parameterTypes = method.getParameters();

            // constructor arguments must match record components
            if (parameterTypes.size() != declaringClass.getDeclaredFields().size()) {
                return false;
            }
            for (int i = 0; i < parameterTypes.size(); i++) {
                TypeDescription.Generic fieldType = declaringClass.getDeclaredFields().get(i).getType();
                if (!fieldType.equals(parameterTypes.get(i).getType())) {
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
                ", methodDescription=" + methodDescription +
                '}';
    }
}
