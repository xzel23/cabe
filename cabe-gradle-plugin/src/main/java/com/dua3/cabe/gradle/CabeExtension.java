package com.dua3.cabe.gradle;

import com.dua3.cabe.processor.Configuration;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import javax.inject.Inject;

/**
 * CabeExtension represents the extension for the Cabe plugin.
 * It provides access to the input directory, output directory, and classpath.
 */
public class CabeExtension {

    private final DirectoryProperty inputDirectory;
    private final DirectoryProperty outputDirectory;
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
        config = objectFactory.property(Configuration.class).value(Configuration.StandardConfig.STANDARD.config());

        // output into the original classes directory
        outputDirectory = objectFactory.directoryProperty();
        outputDirectory.fileProvider(project.provider(() -> ((SourceSetContainer) project.getExtensions().getByName("sourceSets"))
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                .getOutput()
                .getClassesDirs()
                .getSingleFile()));
        // input from classes-cabe-input
        inputDirectory = objectFactory.directoryProperty();
        inputDirectory.set(project.getLayout().getBuildDirectory().dir("classes-cabe-input"));

        project.getLogger().info("{} instance creation success", CabeExtension.class.getSimpleName());
    }

    /**
     * Returns the input directory for the Cabe plugin.
     *
     * @return the input directory as a DirectoryProperty object
     */
    public DirectoryProperty getInputDirectory() {
        return inputDirectory;
    }

    /**
     * Returns the output directory for the Cabe plugin.
     *
     * @return the output directory as a DirectoryProperty object
     */
    public DirectoryProperty getOutputDirectory() {
        return outputDirectory;
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
