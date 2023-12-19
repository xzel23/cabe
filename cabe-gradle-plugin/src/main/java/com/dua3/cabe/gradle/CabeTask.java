package com.dua3.cabe.gradle;


import com.dua3.cabe.processor.ClassFileProcessingFailedException;
import com.dua3.cabe.processor.ClassPatcher;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * This tasks injects assertions for parameters marked as not allowing null values into the source code.
 */
public class CabeTask extends DefaultTask {

    private final ClassPatcher classPatcher = new ClassPatcher();

    /**
     * Class file folder.
     */
    private Path classFolder;

    /**
     * Set class file folder.
     *
     * @param classFolder the class file folder
     */
    public void setClassFolder(Path classFolder) {
        this.classFolder = Objects.requireNonNull(classFolder, "Class folder is null!");
        getLogger().debug("class folder set to {}", this.classFolder);
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

        classPatcher.processFolder(classFolder);
    }
}
