package com.dua3.cabe.processor;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The ClassInfo class represents information about a Java class.
 */
record ClassInfo(String name, boolean isInnerClass, boolean isStaticClass, boolean isInterface, boolean isEnum,
                 boolean isRecord, boolean isDerived, boolean isAnonymousClass, boolean isPublicApi,
                 NullnessOperator nullnessOperator,
                 String assertionsDisabledFlagName, List<MethodInfo> methods, TypeDescription typeDescription) {
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
     * @param classLoader the {@link ClassLoader} instance to use
     * @param className   the fully qualified name of the class
     * @return a {@link ClassInfo} object representing the specified class
     * @throws ClassNotFoundException if the class with the specified name cannot be found
     */
    public static ClassInfo forClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        TypePool typePool = new TypePool.Default(
                new TypePool.CacheProvider.Simple(),
                ClassFileLocator.ForClassLoader.of(classLoader),
                TypePool.Default.ReaderMode.EXTENDED
        );
        TypeDescription typeDescription = typePool.describe(className).resolve();

        int modifiers = typeDescription.getModifiers();
        boolean isInnerClass = PATTERN_INNER_CLASS_NAME.matcher(className).matches();
        boolean isStaticClass = Modifier.isStatic(modifiers);
        boolean isAnonymousClass = isInnerClass && !isStaticClass && PATTERN_ANONYMOUS_CLASS_SUFFIX.matcher(className).matches();
        boolean isInterface = Modifier.isInterface(modifiers);
        boolean isEnum = typeDescription.isEnum();
        boolean isRecord = typeDescription.getSuperClass() != null && typeDescription.getSuperClass().asErasure().represents(Record.class);
        boolean isDerived = typeDescription.getSuperClass() != null && !typeDescription.getSuperClass().asErasure().represents(Object.class) && !isEnum && !isRecord;
        NullnessOperator nullnessOperator = getClassNullnessOperator(classLoader, typeDescription);
        boolean isPublicApi = Modifier.isPublic(modifiers) || hasPublicApiAncestor(classLoader, typeDescription);
        String assertionsDisabledFlagName = getAssertionsDisabledFlagName(typeDescription);

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
                typeDescription);

        typeDescription.getDeclaredMethods().stream()
                .filter(m -> !m.isSynthetic() && !m.isBridge())
                .sorted(Comparator.comparing(MethodDescription.InDefinedShape::getName))
                .map(m -> MethodInfo.forMethod(ci, m))
                .forEach(methods::add);

        return ci;
    }

    private static NullnessOperator getClassNullnessOperator(ClassLoader classLoader, TypeDescription typeDescription) {
        boolean isNullMarked = isAnnotated(typeDescription, NullMarked.class);
        boolean isNullUnmarked = isAnnotated(typeDescription, NullUnmarked.class);
        NullnessOperator classNullness = getNullnessOperator("class", typeDescription.getName(), isNullMarked, isNullUnmarked);
        return classNullness.combineWithParent(() -> getPackageNullnessOperator(classLoader, typeDescription.getPackage().getName()));
    }

    /**
     * Returns the name of the assertion flag field for a given {@link TypeDescription}.
     *
     * @param typeDescription the {@link TypeDescription} to search for the assertion flag field
     * @return the fully qualified name of the assertion flag field, or null if not found
     */
    public static String getAssertionsDisabledFlagName(TypeDescription typeDescription) {
        for (TypeDescription cls = typeDescription; cls != null; cls = cls.getDeclaringType()) {
            // does the current class or one of its nested classes contain the flag?
            FieldDescription flag = Stream.concat(Stream.of(cls), cls.getDeclaredTypes().stream())
                    .map(ClassInfo::getAssertionsDisabledField)
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);
            if (flag != null) {
                return flag.getDeclaringType().getTypeName() + "." + flag.getName();
            }
        }
        return null;
    }

    /**
     * Retrieves the special field `$assertionsDisabled` from the given class if it exists.
     *
     * @param cls the class from which to retrieve the `$assertionsDisabled` field
     * @return the `$assertionsDisabled` field if found, otherwise null
     */
    private static FieldDescription getAssertionsDisabledField(TypeDescription cls) {
        return cls.getDeclaredFields().stream()
                .filter(f -> f.getName().equals("$assertionsDisabled"))
                .filter(f -> f.getDeclaringType().equals(cls)) // filter fields declared in superclasses
                .findFirst().orElse(null);
    }

    /**
     * Checks if the given class or any of its superclasses have a public API ancestor.
     *
     * @param classLoader     the ClassLoader instance to use
     * @param typeDescription the TypeDescription to check
     * @return true if the given class or any of its superclasses have a public API ancestor, false otherwise
     */
    private static boolean hasPublicApiAncestor(ClassLoader classLoader, TypeDescription typeDescription) {
        for (TypeDescription.Generic superClass = typeDescription.getSuperClass(); superClass != null; superClass = superClass.getSuperClass()) {
            if (superClass.asErasure().represents(Object.class)) {
                break;
            }
            if (Modifier.isPublic(superClass.getModifiers())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines the {@link NullnessOperator} for a given package.
     *
     * @param name the name of the package to check for the annotations
     * @return the {@link NullnessOperator}
     */
    private static NullnessOperator getPackageNullnessOperator(ClassLoader classLoader, String name) {
        boolean isNullMarked = false;
        boolean isNullUnmarked = false;
        Supplier<NullnessOperator> parentNullnessSupplier = () -> NullnessOperator.UNSPECIFIED;
        try {
            TypeDescription pkg = TypePool.Default.of(classLoader).describe(name + ".package-info").resolve();
            isNullMarked = isAnnotated(pkg, NullMarked.class);
            isNullUnmarked = isAnnotated(pkg, NullUnmarked.class);
            parentNullnessSupplier = () -> getModuleNullnessOperator(classLoader);
        } catch (Exception e) {
            LOG.fine(() -> "no package-info: " + name);
        }

        NullnessOperator packageNullness = getNullnessOperator("package", name, isNullMarked, isNullUnmarked);
        return packageNullness.combineWithParent(parentNullnessSupplier);
    }

    /**
     * Retrieves the NullnessOperator for a given module.
     *
     * @param classLoader the class pool used to search for the module-info class
     * @return the NullnessOperator for the specified module, based on its annotations
     */
    private static NullnessOperator getModuleNullnessOperator(ClassLoader classLoader) {
        boolean isNullMarked = false;
        boolean isNullUnmarked = false;
        try {
            TypeDescription moduleInfo = TypePool.Default.of(classLoader).describe("module-info").resolve();
            isNullMarked = isAnnotated(moduleInfo, NullMarked.class);
            isNullUnmarked = isAnnotated(moduleInfo, NullUnmarked.class);
        } catch (Exception e) {
            LOG.fine(() -> "module-info not found");
        }

        return getNullnessOperator("module-info", "(anon)", isNullMarked, isNullUnmarked);
    }

    /**
     * Determines the appropriate {@link NullnessOperator} based on the provided nullness annotations.
     *
     * @param entity the name of the entity being checked
     * @param name the name of the specific member (field or method) being checked
     * @param isNullMarked whether the entity is annotated with {@code @NullMarked}
     * @param isNullUnmarked whether the entity is annotated with {@code @NullUnmarked}
     * @return the corresponding {@link NullnessOperator} for the entity's nullness status
     * @throws IllegalStateException if the entity is both marked and unmarked as nullable
     */
    private static NullnessOperator getNullnessOperator(String entity, String name, boolean isNullMarked, boolean isNullUnmarked) {
        LOG.fine( entity + " " + name + " annotations:"
                + (isNullMarked ? " @" + NullMarked.class.getSimpleName() : "")
                + (isNullUnmarked ?  " @" + NullUnmarked.class.getSimpleName() : "")
        );

        if (isNullMarked && isNullUnmarked) {
            throw new IllegalStateException(
                    entity + " " + name + " is annotated with both @" + NullMarked.class.getSimpleName()
                            + " and @" + NullUnmarked.class.getSimpleName()
            );
        }

        if (isNullMarked) {
            return NullnessOperator.MINUS_NULL;
        } else if (isNullUnmarked) {
            return NullnessOperator.UNION_NULL;
        } else {
            return NullnessOperator.NO_CHANGE;
        }
    }

    /**
     * Checks if a given class element is annotated with the specified annotation.
     *
     * @param el         the class element to check for annotation
     * @param annotation the annotation to check for
     * @return true if the class element is annotated with the specified annotation, false otherwise
     */
    private static boolean isAnnotated(TypeDescription el, Class<? extends java.lang.annotation.Annotation> annotation) {
        try {
            return el.getDeclaredAnnotations().stream()
                    .anyMatch(ad -> ad.getAnnotationType().represents(annotation));
        } catch (Exception e) {
            LOG.warning(() -> "annotation not found: " + annotation.getName());
            return false;
        }
    }
}
