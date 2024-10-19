package com.dua3.cabe.processor;

import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

record ParameterInfo(int index, String param, String name, TypeInfo typeInfo, boolean isSynthetic, MethodInfo methodInfo) {
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

        ParameterList<ParameterDescription.InDefinedShape> parms = mi.methodDescription().getParameters();
        int n = parms.size();

        List<ParameterInfo> pi = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String symbol = "$" + (1 + i);
            ParameterDescription.InDefinedShape param = parms.get(i);

            String name = param.isNamed() ? param.getActualName() : "arg#" + param.getIndex();

            boolean isSynthetic = param.isSynthetic()
                    || (mi.isConstructor() && i==0)
                    || (mi.isConstructor() && ci.isInnerClass() && i==1);

            TypeDescription.Generic type = param.getType();

            TypeInfo typeInfo = getTypeInfo(type);
            pi.add(new ParameterInfo(i, symbol, name, typeInfo, isSynthetic, mi));
        }

        return pi;
    }

    private static TypeInfo getTypeInfo(TypeDescription.Generic type) {
        String name = type.getActualName();
        String rawName = type.asRawType().getActualName();
        boolean isNonNullAnnotated = type.getDeclaredAnnotations().stream().anyMatch(a -> a.getAnnotationType().getName().equals(NonNull.class.getName()));
        boolean isNullableAnnotated = type.getDeclaredAnnotations().stream().anyMatch(a -> a.getAnnotationType().getName().equals(Nullable.class.getName()));
        NullnessOperator nullnessOperator = isNonNullAnnotated ? NullnessOperator.MINUS_NULL
                : isNullableAnnotated ? NullnessOperator.UNION_NULL
                : NullnessOperator.UNSPECIFIED;
        return new TypeInfo(name, rawName, nullnessOperator);
    }

    public static boolean isPrimitive(String type) {
        return PRIMITIVES.contains(type);
    }

    @Override
    public String toString() {
        return "ParameterInfo{" +
                "param='" + param + '\'' +
                ", name='" + name + '\'' +
                ", typeInfo='" + typeInfo + '\'' +
                ", isSynthetic=" + isSynthetic +
                ", methodInfo=" + methodInfo.name() +
                '}';
    }
}
