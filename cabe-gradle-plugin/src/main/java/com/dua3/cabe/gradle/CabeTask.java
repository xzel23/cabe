package com.dua3.cabe.gradle;


import com.dua3.cabe.processor.ClassPatcher;
import com.dua3.cabe.processor.Config;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This tasks injects assertions for parameters marked as not allowing null values into the source code.
 */
public abstract class CabeTask extends DefaultTask {
    private final DirectoryProperty inputDirectory;
    private final DirectoryProperty outputDirectory;
    private final Provider<FileCollection> classPath;
    private final Property<Config> config;

    /**
     * This task injects assertions for parameters marked as not allowing null values into the source code.
     *
     * @param objectFactory the Gradle object factory
     */
    @Inject
    public CabeTask(ObjectFactory objectFactory) {
        // Properties are created via Gradle's ObjectFactory
        inputDirectory = objectFactory.directoryProperty();
        outputDirectory = objectFactory.directoryProperty();
        classPath = objectFactory.property(FileCollection.class);
        config = objectFactory.property(Config.class);
    }

    /**
     * Retrieves the input directory for the Cabe task.
     *
     * @return The input directory for the Cabe task.
     */
    @InputFiles
    public DirectoryProperty getInputDirectory() {
        return inputDirectory;
    }

    /**
     * Retrieves the output directory for the Cabe task.
     *
     * @return The output directory for the Cabe task.
     */
    @OutputDirectory
    public DirectoryProperty getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Retrieves the class path for the Cabe task.
     *
     * @return The class path for the Cabe task.
     */
    @Classpath
    public Property<FileCollection> getClassPath() {
        return (Property<FileCollection>) classPath;
    }

    @Input
    public Property<Config> getConfig() {
        return config;
    }

    @TaskAction
    void run() {
        try {
            List<Path> classpath = getClassPath().get().getFiles().stream()
                    .map(File::toPath)
                    .collect(Collectors.toList());
            Config configuration = config.get();
            ClassPatcher classPatcher = new ClassPatcher(classpath, configuration);
            classPatcher.processFolder(
                    getInputDirectory().get().getAsFile().toPath(),
                    getOutputDirectory().get().getAsFile().toPath()
            );
        } catch (Exception e) {
            throw new GradleException("An error occurred while instrumenting classes", e);
        }
    }

}
