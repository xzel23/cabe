package com.dua3.cabe.gradle;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;

import javax.inject.Inject;

/**
 * CabeExtension represents the extension for the Cabe plugin.
 * It provides access to the input directory, output directory, and classpath.
 */
public class CabeExtension {

    private final DirectoryProperty inputDirectory;
    private final DirectoryProperty outputDirectory;
    private final Provider<FileCollection> classPath;

    /**
     * Construct a new instance of the extension.
     *
     * @param project the project to configure
     * @param projectLayout the project layout
     */
    @Inject
    public CabeExtension(Project project, ProjectLayout projectLayout) {
        ObjectFactory objectFactory = project.getObjects();

        // output into the original classes directory
        outputDirectory = objectFactory.directoryProperty();
        outputDirectory.fileProvider(project.provider(() -> project.getConvention().getPlugin(JavaPluginConvention.class)
                .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                .getOutput().getClassesDirs()
                .getSingleFile()));
        // input from classes-cabe-input
        inputDirectory = objectFactory.directoryProperty()
                .convention(projectLayout.getBuildDirectory().dir("classes-cabe-input"));
        // compile classpath
        classPath = objectFactory.property(FileCollection.class)
                .value(project.provider(
                        () -> project.getConvention().getPlugin(JavaPluginConvention.class)
                                .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getCompileClasspath()));
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
     * Retrieves the classpath for the Cabe plugin.
     *
     * @return the classpath as a Provider object of type FileCollection
     */
    public Provider<FileCollection> getClassPath() {
        return classPath;
    }

}
