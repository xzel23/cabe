package com.dua3.cabe.processor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Utility class providing various helper methods for nullness operators and assertions.
 */
public final class Util {
    private static final Logger LOG = Logger.getLogger(Util.class.getName());

    private Util() {
        // utility class
    }

    /**
     * Determines the {@link NullnessOperator} based on the provided annotations.
     *
     * @param annotations Array of annotations to analyze for nullness information.
     * @return The {@link NullnessOperator} derived from the annotations.
     * @throws IllegalStateException if both {@link NullMarked} and {@link NullUnmarked} annotations are present.
     */
    public static NullnessOperator getNullnessOperator(Annotation[] annotations) {
        boolean isNullMarked = isAnnotationPresent(annotations, NullMarked.class) || isAnnotationPresent(annotations, NonNull.class);
        boolean isNullUnmarked = isAnnotationPresent(annotations, NullUnmarked.class) || isAnnotationPresent(annotations, Nullable.class);
        return getNullnessOperator(isNullMarked, isNullUnmarked);
    }

    private static NullnessOperator getNullnessOperator(boolean isNullMarked, boolean isNullUnmarked) {
        if (isNullMarked && isNullUnmarked) {
            throw new IllegalStateException(
                    "both "
                            + "@" + NullMarked.class.getSimpleName() + "/@" + NonNull.class.getSimpleName()
                            + " and @" + NullUnmarked.class.getSimpleName() + "/@" + Nullable.class.getSimpleName()
                            + " are present"
            );
        }

        if (isNullMarked) {
            return com.dua3.cabe.processor.NullnessOperator.MINUS_NULL;
        } else if (isNullUnmarked) {
            return com.dua3.cabe.processor.NullnessOperator.UNION_NULL;
        } else {
            return com.dua3.cabe.processor.NullnessOperator.NO_CHANGE;
        }
    }

    @SuppressWarnings("java:S1872")
    private static boolean isAnnotationPresent(Annotation[] annotations, Class<?> annotation) {
        for (Annotation a: annotations) {
            // DO NOT COMPARE CLASSES! The object trees are disjunct, they use unrelated class loaders.
            if (a.annotationType().getName().equals(annotation.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the name of the assertion flag field for a given {@link Class}.
     *
     * @param cls the {@link Class} to search for the assertion flag field
     * @return the fully qualified name of the assertion flag field, or null if not found
     */
    public static String getAssertionsDisabledFlagName(Class<?> cls) {
        for (Class<?> currentClass = cls; currentClass != null; currentClass = currentClass.getDeclaringClass()) {
            // does the current class or one of its nested classes contain the flag?
            Field flag = Stream.concat(Stream.of(currentClass), Arrays.stream(currentClass.getDeclaredClasses()))
                    .map(Util::getAssertionsDisabledField)
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);
            if (flag != null) {
                return flag.getDeclaringClass().getTypeName() + "." + flag.getName();
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
    public static Field getAssertionsDisabledField(Class<?> cls) {
        return Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.getName().equals("$assertionsDisabled"))
                .filter(f -> f.getDeclaringClass().equals(cls)) // filter fields declared in superclasses
                .findFirst().orElse(null);
    }

    /**
     * Checks if the given class or any of its superclasses have a public API ancestor.
     *
     * @param cls the TypeDescription to check
     * @return true if the given class or any of its superclasses have a public API ancestor, false otherwise
     */
    @SuppressWarnings("java:S1872")
    public static boolean hasPublicApiAncestor(Class<?> cls) {
        for (Class<?> superClass = cls.getSuperclass(); superClass != null; superClass = superClass.getSuperclass()) {
            // DO NOT COMPARE CLASSES! The object trees are disjunct, they use unrelated class loaders.
            assert superClass.getName().equals(Object.class.getName()) == Objects.equals(superClass, Object.class);
            if (superClass.getName().equals(Object.class.getName())) {
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
     * @param cl     the class loader to use
     * @param pkg    the package to check for the annotations
     * @param mod    the module the package belongs to
     * @return the {@link NullnessOperator}
     */
    static NullnessOperator getPackageNullnessOperator(ClassLoader cl, Package pkg, Module mod) {
        NullnessOperator packageNullness = getNullnessOperator(pkg.getDeclaredAnnotations());
        Supplier<NullnessOperator> parentNullnessSupplier = () -> getModuleNullnessOperator(cl, mod, pkg.getName());
        return packageNullness.combineWithParent(parentNullnessSupplier);
    }

    private static NullnessOperator getModuleNullnessOperator(ClassLoader cl, Module mod, String packageName) {
        if (mod.isNamed()) {
            return getNullnessOperator(mod.getDeclaredAnnotations());
        }

        if (cl instanceof ModuleClassLoader mcl) {
            return mcl.getModuleNameForPackage(packageName)
                    .flatMap(mcl::getPathForModule)
                    .map(Util::getModuleNullnessOperatorFromFile)
                    .orElse(getNullnessOperator(mod.getDeclaredAnnotations()));
        }

        return getNullnessOperator(mod.getDeclaredAnnotations());
    }

    /**
     * Determines the {@link NullnessOperator} for a given class.
     *
     * @param cls the {@link Class} for which the nullness operator is to be determined
     * @return the {@link NullnessOperator} representing the nullness state of the class
     */
    public static NullnessOperator getClassNullnessOperator(Class<?> cls) {
        NullnessOperator classNullness = getNullnessOperator(cls.getDeclaredAnnotations());
        return classNullness.combineWithParent(() -> {
            Package pkg = cls.getPackage();
            Module mod = cls.getModule();
            ClassLoader cl = cls.getClassLoader();
            return getPackageNullnessOperator(cl, pkg, mod);
        });
    }

    private static NullnessOperator getModuleNullnessOperatorFromFile(Path modulePath) {
        if (Files.isDirectory(modulePath)) {
            Path moduleInfoPath = modulePath.resolve("module-info.class");
            if (Files.exists(moduleInfoPath)) {
                return readNullnessOperatorFromClassFile(moduleInfoPath);
            }
        } else if (Files.isRegularFile(modulePath) && modulePath.toString().endsWith(".jar")) {
            try (FileSystem jarFs = FileSystems.newFileSystem(modulePath, (ClassLoader) null)) {
                Path moduleInfoPath = jarFs.getPath("/module-info.class");
                if (Files.exists(moduleInfoPath)) {
                    return readNullnessOperatorFromClassFile(moduleInfoPath);
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, e, () -> "Failed to read module-info.class from JAR " + modulePath);
            }
        }
        return NullnessOperator.NO_CHANGE;
    }

    private static NullnessOperator readNullnessOperatorFromClassFile(Path classFilePath) {
        try {
            // We use javassist to read annotations from the class file
            javassist.bytecode.ClassFile cf;
            try (InputStream is = Files.newInputStream(classFilePath)) {
                cf = new javassist.bytecode.ClassFile(new java.io.DataInputStream(is));
            }
            javassist.bytecode.AnnotationsAttribute visible = (javassist.bytecode.AnnotationsAttribute) cf.getAttribute(javassist.bytecode.AnnotationsAttribute.visibleTag);
            javassist.bytecode.AnnotationsAttribute invisible = (javassist.bytecode.AnnotationsAttribute) cf.getAttribute(javassist.bytecode.AnnotationsAttribute.invisibleTag);

            boolean isNullMarked = isAnnotationPresent(visible, NullMarked.class) || isAnnotationPresent(invisible, NullMarked.class)
                    || isAnnotationPresent(visible, NonNull.class) || isAnnotationPresent(invisible, NonNull.class);
            boolean isNullUnmarked = isAnnotationPresent(visible, NullUnmarked.class) || isAnnotationPresent(invisible, NullUnmarked.class)
                    || isAnnotationPresent(visible, Nullable.class) || isAnnotationPresent(invisible, Nullable.class);

            return getNullnessOperator(isNullMarked, isNullUnmarked);
        } catch (IOException e) {
            LOG.log(Level.WARNING, e, () -> "Failed to read class file from " + classFilePath);
        }
        return NullnessOperator.NO_CHANGE;
    }

    private static boolean isAnnotationPresent(javassist.bytecode.AnnotationsAttribute attribute, Class<?> annotationClass) {
        return attribute != null && attribute.getAnnotation(annotationClass.getName()) != null;
    }
}
