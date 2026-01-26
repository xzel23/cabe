package com.dua3.cabe.processor;

import java.lang.module.*;
import java.lang.module.Configuration;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A custom {@link ClassLoader} that loads classes from specified module paths.
 * This class resolves modules from the provided paths and creates a module layer
 * which is used to load the classes. It maintains a registry of classes and their
 * associated modules to facilitate class loading.
 */
public class ModuleClassLoader extends ClassLoader {
    private static final Logger LOG = Logger.getLogger(ModuleClassLoader.class.getName());
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

        Configuration configuration;
        try {
            // attempt to resolve and bind the modules
            configuration = parentLayer.configuration()
                    .resolveAndBind(finder, ModuleFinder.of(), moduleToPath.keySet());
        } catch (ResolutionException e) {
            // resolveAndBind() can fail if a module has optional dependencies (requires static)
            // that are missing from the module path at runtime. Falling back to resolve()
            // will skip service binding and optional dependency resolution, allowing
            // the module layer to be created anyway.
            LOG.log(Level.INFO, "Module resolution failed using resolveAndBind(), falling back to resolve(): {0}", e.getMessage());
            try {
                configuration = parentLayer.configuration()
                        .resolve(finder, ModuleFinder.of(), moduleToPath.keySet());
            } catch (ResolutionException e2) {
                LOG.log(Level.WARNING, "Module resolution failed using resolve(): {0}. Instrumentation will proceed without a full module layer.", e2.getMessage());
                configuration = null;
            }
        }

        if (configuration != null) {
            ModuleLayer.Controller controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(parentLayer), parent);
            moduleLayer = controller.layer();
        } else {
            moduleLayer = null;
        }
    }

    /**
     * Returns the {@link ModuleLayer} created by this class loader.
     * @return the module layer, or null if module resolution failed
     */
    public ModuleLayer getModuleLayer() {
        return moduleLayer;
    }

    /**
     * Returns the name of the module that contains the given package.
     * @param packageName the name of the package
     * @return an {@link Optional} containing the module name, or an empty {@link Optional} if the package is not found
     */
    public Optional<String> getModuleNameForPackage(String packageName) {
        return Optional.ofNullable(packageToModuleName.get(packageName));
    }

    /**
     * Returns the path to the given module.
     * @param moduleName the name of the module
     * @return an {@link Optional} containing the path to the module, or an empty {@link Optional} if the module is not found
     */
    public Optional<Path> getPathForModule(String moduleName) {
        return Optional.ofNullable(moduleToPath.get(moduleName));
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

        if (moduleLayer != null) {
            Module module =
                    moduleLayer.findModule(moduleName)
                    .orElseThrow(() -> new ClassNotFoundException("class " + name + " not found (module " + moduleName + ")"));
            return module.getClassLoader().loadClass(name);
        } else {
            return getParent().loadClass(name);
        }
    }
}
