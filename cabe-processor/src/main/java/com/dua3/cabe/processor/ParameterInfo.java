package com.dua3.cabe.processor;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

record ParameterInfo(int index, String param, String name, Class<?> type, NullnessOperator nullnessOperator, boolean isSynthetic, MethodInfo methodInfo) {
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

        Executable methodDescription = mi.method();
        Parameter[] parms = methodDescription.getParameters();
        int n = parms.length;

        List<ParameterInfo> pi = new ArrayList<>();
        for (int i = 0, j = 0; i < n; i++) {
            String symbol = "$" + (1 + i);
            Parameter param = parms[i];
            String name = param.isNamePresent() ? param.getName() : "arg#" + (j + 1);

            boolean isSynthetic = param.isSynthetic()
                    || (mi.isConstructor() && !ci.isStaticClass() && ci.isInnerClass() && i==0);

            AnnotatedType type = param.getAnnotatedType();

            NullnessOperator nullnessOperator = Util.getNullnessOperator(param.getDeclaredAnnotations())
                            .combineWithParent(() -> Util.getNullnessOperator(type.getDeclaredAnnotations()));
            pi.add(new ParameterInfo(i, symbol, name, param.getType(), nullnessOperator, isSynthetic, mi));

            if (!isSynthetic) {
                j++;
            }
        }

        return pi;
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
                ", nullnessOperator=" + nullnessOperator +
                ", isSynthetic=" + isSynthetic +
                ", methodInfo=" + methodInfo.name() +
                '}';
    }
}
