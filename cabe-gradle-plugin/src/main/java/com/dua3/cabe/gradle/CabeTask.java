package com.dua3.cabe.gradle;


import com.dua3.cabe.notnull.JetrainsAnnotationsNotNullProcessor;
import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import spoon.Launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class CabeTask extends DefaultTask {

    /** The latest Java version supported (by SPOON). */
    public static final int MAX_COMPATIBLE_JAVA_VERSION = 16;
    
    /** Source folders. */
    private final Collection<String> srcFolders = new ArrayList<>();

    /** Output folder for generated sources. */
    @Internal
    private File outFolder = null;

    /** Omit classpath (see SPOON docs). */
    private boolean noClasspath = false;

    /** SPOON compliance level. */
    private String compliance = JavaVersion.current().getMajorVersion();

    /** The class path. */
    @Classpath
    private FileCollection classpath = null;

    /**
     * Set source folders.
     * 
     * @param srcFolders the source folders
     */
    public void setSrcFolders(Collection<String> srcFolders) {
        this.srcFolders.clear();
        this.srcFolders.addAll(srcFolders);
        getProject().getLogger().debug("cabe source folder(s) set to {}", this.srcFolders);
    }

    /**
     * Set output folder.
     * 
     * @param outFolder the output folder
     */
    public void setOutFolder(File outFolder) {
        this.outFolder = Objects.requireNonNull(outFolder);
        getProject().getLogger().debug("cabe output folder set to {}", this.outFolder);
    }

    /**
     * Set output folder.
     */
     public File getOutFolder() {
        return Objects.requireNonNull(outFolder, "outputfolfer has not yet been set");
    }

    /** 
     * Get class path.
     */
    public FileCollection getClasspath() {
        return classpath;
    }

    /** 
     * Set class path. 
     * 
     * @param classpath the class path
     */
    public void setClasspath(FileCollection classpath) {
        this.classpath = Objects.requireNonNull(classpath);
    }

    /**
     * Set the Java version. If the Java version is supported by SPOON, it is passed on unchanged. Otherwise,
     * the latest Java version supported by SPOON is used.
     * 
     * @param jv The Javaversion used to compile the project sources 
     */
    public void setJavaVersionCompliance(JavaVersion jv) {
        int majorVersion = Integer.parseInt(jv.getMajorVersion());
        int maxVersion = Math.min(MAX_COMPATIBLE_JAVA_VERSION, jv.ordinal() + 1);

        if (maxVersion != majorVersion) {
            getProject().getLogger().warn(
                    "Project target is Java {} but source code transformation max supported version is {}", 
                    majorVersion, 
                    maxVersion
            );   
        }
        
        compliance = Integer.toString(maxVersion);
        getProject().getLogger().debug("source code transformation uses Java {} compliance", maxVersion);
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
                "--lines",
                "--with-imports",
                "--output-type", "compilationunits",
                "--compliance", compliance
        ));
        
        if (noClasspath) {
            params.add("-x");
        }

        params.add("-p");
        params.add(JetrainsAnnotationsNotNullProcessor.class.getName());

        if (!classpath.isEmpty()) {
            params.add("--source-classpath");
            params.add(classpath.getAsPath());
        }

        Launcher launcher = new Launcher();
        getProject().getLogger().debug("calling SPOON launcher with args: {}", params);
        launcher.setArgs(params.toArray(new String[params.size()]));
        launcher.run();
    }

}
