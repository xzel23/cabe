package com.dua3.cabe.processor;

import com.dua3.cabe.annotations.NotNull;
import com.dua3.cabe.annotations.Nullable;
import javassist.CtBehavior;
import javassist.bytecode.Descriptor;
import javassist.bytecode.LocalVariableAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

record ParameterInfo(String param, String name, String type, boolean isSynthetic, MethodInfo methodInfo,
                     boolean isNotNullAnnotated, boolean isNullableAnnotated) {
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

    public static List<ParameterInfo> forMethod(MethodInfo mi) {
        ClassInfo ci = mi.classInfo();

        String[] types = getParameterTypes(mi.ctMethod());
        int n = types.length;
        int syntheticParameterCount = 0;
        int extraSyntheticParameters = 0;
        int parameterOffset = 0;
        boolean isEnumConstructor = ci.isEnum() && mi.isConstructor();
        if (isEnumConstructor) {
            extraSyntheticParameters += 2;
            parameterOffset += 1;
            n -= 2;
        }
        if (mi.isConstructor() && ci.isInnerClass() && !ci.isStaticClass()) {
            extraSyntheticParameters += 1;
            n--;
        }

        var methodInfo = mi.ctMethod().getMethodInfo();
        var ca = methodInfo.getCodeAttribute();
        if (ca == null) {
            return Collections.emptyList();
        }
        var lva = (LocalVariableAttribute) ca.getAttribute(LocalVariableAttribute.tag);
        Object[][] parameterAnnotations;
        try {
            parameterAnnotations = mi.ctMethod().getParameterAnnotations();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        List<ParameterInfo> pi = new ArrayList<>();
        // i is the global index, k is the number of non-synthetic parameters that have been added
        for (int i = 0, j = 0, k = 0; i < syntheticParameterCount || extraSyntheticParameters > 0 || k < n; i++) {
            String param = "_";
            String name = getParameterName(lva, syntheticParameterCount + extraSyntheticParameters, i, parameterOffset);

            boolean isSynthetic = i < syntheticParameterCount + extraSyntheticParameters;
            String type = "?";

            boolean isNotNullAnnotated = false;
            boolean isNullableAnnotated = false;
            if (!isSynthetic && PATTERN_SYNTHETIC_PARAMETER_NAMES.matcher(name).matches()) {
                isSynthetic = true;
                syntheticParameterCount++;
            } else if (extraSyntheticParameters > 0) {
                isSynthetic = true;
                syntheticParameterCount++;
                extraSyntheticParameters--;
                j++;
            } else {
                param = "$" + (k + j + 1);
                type = types[k + j];
                for (Object annotation : parameterAnnotations[k]) {
                    isNotNullAnnotated = isNotNullAnnotated || (annotation instanceof NotNull);
                    isNullableAnnotated = isNullableAnnotated || (annotation instanceof Nullable);
                }
                k++;
            }

            pi.add(new ParameterInfo(param, name, type, isSynthetic, mi, isNotNullAnnotated, isNullableAnnotated));
        }
        return pi;
    }

    /**
     * Retrieves the parameter types of a given method.
     *
     * @param method the method to retrieve parameter types for
     * @return an array of Strings representing the parameter types of the method
     * @throws IllegalStateException if the parameter descriptor is malformed
     */
    private static String[] getParameterTypes(CtBehavior method) {
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
     * Retrieves the name of a parameter in a method.
     *
     * @param lva the LocalVariableAttribute representing the method's local variables
     * @param i   the index of the parameter to retrieve the name for
     * @return the name of the parameter at the specified index
     */
    private static String getParameterName(LocalVariableAttribute lva, int syntheticParameterCount, int i, int offset) {
        if (i < syntheticParameterCount) {
            return "_" + (i + 1);
        }
        for (int j = 0; j < lva.tableLength(); j++) {
            if (lva.index(j) == i + offset) {
                return lva.variableName(j);
            }
        }
        return "param#" + (i + 1 - syntheticParameterCount);
    }

    public static boolean isPrimitive(String type) {
        return PRIMITIVES.contains(type);
    }

    @Override
    public String toString() {
        return "ParameterInfo{" +
                "param='" + param + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", isSynthetic=" + isSynthetic +
                ", methodInfo=" + methodInfo.name() +
                ", isNotNullAnnotated=" + isNotNullAnnotated +
                ", isNullableAnnotated=" + isNullableAnnotated +
                '}';
    }
}
