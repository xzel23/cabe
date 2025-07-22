package com.dua3.cabe.gradle;

import com.dua3.cabe.processor.Configuration;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

/**
 * CabeExtension represents the extension for the Cabe plugin.
 * It provides access to the input directory, output directory, and classpath.
 */
public class CabeExtension {

    private final DirectoryProperty originalClassesDirectory;
    private final DirectoryProperty classesDirectory;
    private final Property<Configuration> config;
    private final Property<Integer> verbosity;

    /**
     * Construct a new instance of the extension.
     *
     * @param project the project to configure
     */
    @Inject
    public CabeExtension(Project project) {
        ObjectFactory objectFactory = project.getObjects();

        // get value of verbosity
        verbosity = objectFactory.property(Integer.class).value(0);

        // get value of config
        config = objectFactory.property(Configuration.class).value(Configuration.STANDARD);

        classesDirectory = objectFactory.directoryProperty();
        originalClassesDirectory = objectFactory.directoryProperty();

        project.getLogger().info("{} instance creation success", CabeExtension.class.getSimpleName());
    }

    /**
     * Returns the input directory for the Cabe plugin.
     *
     * @return the input directory as a DirectoryProperty object
     */
    public DirectoryProperty getOriginalClassesDirectory() {
        return originalClassesDirectory;
    }

    /**
     * Returns the output directory for the Cabe plugin.
     *
     * @return the output directory as a DirectoryProperty object
     */
    public DirectoryProperty getClassesDirectory() {
        return classesDirectory;
    }

    /**
     * Retrieves the configuration property for the Cabe plugin.
     *
     * @return the configuration property as a Property object of type Configuration
     */
    public Property<Configuration> getConfig() {
        return config;
    }

    /**
     * Sets the configuration property for the Cabe plugin.
     *
     * @param config the configuration to set
     */
    public void setConfig(Configuration config) {
        this.config.set(config);
    }

    /**
     * Retrieves the configuration property for the Cabe plugin.
     *
     * @return the configuration property as a Property object of type Configuration
     */
    public Property<Integer> getVerbosity() {
        return verbosity;
    }

    /**
     * Sets the configuration property for the Cabe plugin.
     *
     * @param verbosity the verbosity level to set
     */
    public void setVerbosity(int verbosity) {
        this.verbosity.set(verbosity);
    }
}
