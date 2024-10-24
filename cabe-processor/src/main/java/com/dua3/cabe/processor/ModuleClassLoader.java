package com.dua3.cabe.processor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.*;
import java.lang.module.Configuration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A custom {@link ClassLoader} that loads classes from specified module paths.
 * This class resolves modules from the provided paths and creates a module layer
 * which is used to load the classes. It maintains a registry of classes and their
 * associated modules to facilitate class loading.
 */
public class ModuleClassLoader extends ClassLoader {
    private final Map<String, Module> classModuleRegistry;

    /**
     * Constructs a new ModuleClassLoader instance. This class loader is capable of
     * loading classes from specified module paths. It creates a new module layer
     * using the given paths and resolves the modules which are then used for class
     * loading.
     *
     * @param parent the parent class loader for delegation
     * @param paths the paths to the modules to be loaded
     * @throws IOException if an I/O error occurs when accessing the module paths
     */
    public ModuleClassLoader(ClassLoader parent, Path... paths) throws IOException {
        super(parent);
        this.classModuleRegistry = new HashMap<>();

        ModuleFinder finder = ModuleFinder.of(paths);
        Set<ModuleReference> moduleReferences = finder.findAll();

        Set<String> moduleNames = moduleReferences.stream()
                .map(ModuleReference::descriptor)
                .map(ModuleDescriptor::name)
                .collect(Collectors.toSet());

        ModuleLayer parentLayer = ModuleLayer.boot();

        Configuration configuration = parentLayer.configuration()
                .resolveAndBind(finder, ModuleFinder.of(), moduleNames);

        ModuleLayer.Controller controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(parentLayer), parent);

        populateClassModuleRegistry(controller.layer());
    }

    private void populateClassModuleRegistry(ModuleLayer moduleLayer) throws IOException {
        try {
            moduleLayer.modules().forEach(module -> {
                module.getPackages().forEach(pkg -> {
                    String packageName = pkg.replace('.', '/');
                    Path packagePath = Path.of(packageName);
                    try (Stream<Path> paths = Files.walk(packagePath)) {
                        paths.filter(Files::isRegularFile)
                                .map(Path::toString)
                                .filter(name -> name.endsWith(".class"))
                                .map(name -> name.replaceFirst("\\.class$", "").replace('/', '.'))
                                .forEach(className -> classModuleRegistry.put(className, module));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Module module = classModuleRegistry.get(name);
        if (module == null) {
            throw new ClassNotFoundException("Class " + name + " not found in any module");
        }
        return module.getClassLoader().loadClass(name);
    }
}