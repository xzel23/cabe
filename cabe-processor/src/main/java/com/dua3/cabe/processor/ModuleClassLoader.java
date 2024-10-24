package com.dua3.cabe.processor;

import java.lang.module.*;
import java.lang.module.Configuration;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A custom {@link ClassLoader} that loads classes from specified module paths.
 * This class resolves modules from the provided paths and creates a module layer
 * which is used to load the classes. It maintains a registry of classes and their
 * associated modules to facilitate class loading.
 */
public class ModuleClassLoader extends ClassLoader {
    private final Map<String, String> packageToModuleName;
    private final Map<String, Path> moduleToPath;
    private final ModuleLayer moduleLayer;

    /**
     * Constructs a new ModuleClassLoader instance. This class loader is capable of
     * loading classes from specified module paths. It creates a new module layer
     * using the given paths and resolves the modules which are then used for class
     * loading.
     *
     * @param parent the parent class loader for delegation
     * @param paths the paths to the modules to be loaded
     */
    public ModuleClassLoader(ClassLoader parent, Path... paths) {
        super(parent);

        ModuleFinder finder = ModuleFinder.of(paths);
        Set<ModuleReference> moduleReferences = finder.findAll();
        packageToModuleName = new HashMap<>();
        moduleToPath = new HashMap<>();
        moduleReferences.forEach(mr -> {
            final String moduleName = mr.descriptor().name();
            Path path = mr.location().map(Paths::get).orElse(null);
            Path oldPath = moduleToPath.put(moduleName, path);

            if (oldPath != null) {
                throw new IllegalStateException("module " + moduleName + " is defined in paths " + oldPath + " and " + path);
            }

            mr.descriptor().packages().forEach(packageName -> {
                String oldModule = packageToModuleName.put(packageName, moduleName);
                if (oldModule != null) {
                    throw new IllegalStateException("package " + packageName + " is defined in modules " + oldModule + " and " + moduleName);
                }
            });
        });

        ModuleLayer parentLayer = ModuleLayer.boot();

        Configuration configuration = parentLayer.configuration()
                .resolveAndBind(finder, ModuleFinder.of(), moduleToPath.keySet());

        ModuleLayer.Controller controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(parentLayer), parent);
        moduleLayer = controller.layer();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            throw new ClassNotFoundException("class " + name + " not found (no package specified)");
        }

        String pkg = name.substring(0, lastDot);
        String moduleName = packageToModuleName.get(pkg);

        if (moduleName == null) {
            throw new ClassNotFoundException("class " + name + " not found");
        }
        Module module =
                moduleLayer.findModule(moduleName)
                .orElseThrow(() -> new ClassNotFoundException("class " + name + " not found (module " + moduleName + ")"));
        return module.getClassLoader().loadClass(name);
    }
}
