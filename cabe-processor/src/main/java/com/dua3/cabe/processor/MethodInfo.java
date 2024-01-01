package com.dua3.cabe.processor;

import javassist.CtBehavior;
import javassist.Modifier;
import javassist.bytecode.AccessFlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents information about a method.
 */
record MethodInfo(String name, boolean isConstructor, boolean isMethod, boolean isAbstract, boolean isStatic,
                  boolean isSynthetic, boolean isBridge, boolean isNative,
                  List<ParameterInfo> parameters, ClassInfo classInfo, CtBehavior ctMethod) {
    public static MethodInfo forMethod(ClassInfo ci, CtBehavior ctMethod) {
        var methodInfo = ctMethod.getMethodInfo();
        boolean isConstructor = methodInfo.isConstructor();
        boolean isMethod = methodInfo.isMethod();
        boolean isAbstract = Modifier.isAbstract(ctMethod.getModifiers());
        boolean isStatic = Modifier.isStatic(ctMethod.getModifiers());

        int accessFlags = ctMethod.getMethodInfo().getAccessFlags();
        boolean isSynthetic = (accessFlags & AccessFlag.SYNTHETIC) != 0;
        boolean isBridge = (accessFlags & AccessFlag.BRIDGE) != 0;
        boolean isNative = (accessFlags & AccessFlag.NATIVE) != 0;

        List<ParameterInfo> parameters = new ArrayList<>();

        MethodInfo mi = new MethodInfo(
                ctMethod.getLongName(),
                isConstructor,
                isMethod,
                isAbstract,
                isStatic,
                isSynthetic,
                isBridge,
                isNative,
                parameters,
                ci,
                ctMethod
        );

        parameters.addAll(ParameterInfo.forMethod(mi));

        return mi;
    }

    @Override
    public List<ParameterInfo> parameters() {
        return Collections.unmodifiableList(parameters);
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
