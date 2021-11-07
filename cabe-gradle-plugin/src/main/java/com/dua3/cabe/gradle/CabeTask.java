package com.dua3.cabe.gradle;


import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.TaskAction;
import spoon.Launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class CabeTask extends DefaultTask {
    Collection<String> srcFolders = new ArrayList<>();
    File outFolder = null;
    int compliance = JavaVersion.current().ordinal();

    boolean noClasspath = false;

    @Classpath
    FileCollection classpath = null;

    public FileCollection getClasspath() {
        return classpath;
    }

    public void setClasspath(FileCollection classpath) {
        this.classpath = classpath;
    }

    @TaskAction
    void run() {
        Logger log = getProject().getLogger();
        
        // No source code to spoon.
        if (srcFolders.isEmpty()) {
            log.debug("cabe: no source folders");
            return;
        }

        List<String> params = new LinkedList<>(List.of(
                "-i", String.join(File.pathSeparator, srcFolders),
                "-o", outFolder.getAbsolutePath(),
                "--compliance", Integer.toString(Math.min(16, compliance))
        ));
        
        if (noClasspath) {
            params.add("-x");
        }

        params.add("-p");
        params.add(NotNullProcessor.class.getName());

        if (!classpath.isEmpty()) {
            params.add("--source-classpath");
            params.add(classpath.getAsPath());
        }

        Launcher launcher = new Launcher();
        launcher.setArgs(params.toArray(new String[params.size()]));
        launcher.run();
    }

}
