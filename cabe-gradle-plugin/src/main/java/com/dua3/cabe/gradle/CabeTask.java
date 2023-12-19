package com.dua3.cabe.gradle;


import com.dua3.cabe.processor.ClassFileProcessingFailedException;
import com.dua3.cabe.processor.ClassPatcher;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This tasks injects assertions for parameters marked as not allowing null values into the source code.
 */
public class CabeTask extends DefaultTask {

    /**
     * Class file folder.
     */
    private Path classFolder;

    private FileCollection compileClasspath;

    /**
     * Set class file folder.
     *
     * @param classFolder the class file folder
     */
    public void setClassFolder(Path classFolder) {
        this.classFolder = Objects.requireNonNull(classFolder, "Class folder is null!");
        getLogger().debug("class folder set to {}", this.classFolder);
    }

    /**
     * Retrieves the compile classpath for the CabeTask.
     *
     * @return the compile classpath as a FileCollection
     * @see CabeTask
     */
    @Classpath
    public FileCollection getCompileClasspath() {
        return this.compileClasspath;
    }

    /**
     * Sets the compile classpath for the CabeTask.
     *
     * @param compileClasspath the compile classpath to be set
     * @see CabeTask
     */
    public void setCompileClasspath(FileCollection compileClasspath) {
        this.compileClasspath = compileClasspath;
    }

    @TaskAction
    void run() throws IOException, ClassFileProcessingFailedException {
        Logger log = getLogger();
        log.debug("running cabe task");

        // no class folder.
        if (classFolder == null) {
            log.warn("no class folder");
            return;
        }

        // no directory
        if (!Files.isDirectory(classFolder)) {
            log.warn("Does not exist or is not a directory: " + classFolder);
            return;
        }

        List<Path> classpath = getCompileClasspath().getFiles().stream()
                .map(File::toPath)
                .collect(Collectors.toList());
        ClassPatcher classPatcher = new ClassPatcher(classpath);
        classPatcher.processFolder(classFolder);
    }
}
