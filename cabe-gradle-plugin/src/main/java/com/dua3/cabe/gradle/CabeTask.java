package com.dua3.cabe.gradle;


import com.dua3.cabe.spoon.CabeSpoonProcessor;
import com.dua3.cabe.spoon.notnull.CabeAnnotationsNotNullProcessor;
import com.dua3.cabe.spoon.notnull.JetBrainsAnnotationsNotNullProcessor;
import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import spoon.Launcher;
import spoon.OutputType;
import spoon.compiler.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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

    /** SPOON compliance level. */
    private int compliance = Math.min(MAX_COMPATIBLE_JAVA_VERSION, getMajorVersion(JavaVersion.current()));

    /** The class path. */
    @Classpath
    private FileCollection classpath = null;

    private final List<CabeSpoonProcessor> PROCESSORS = List.of(
            new CabeAnnotationsNotNullProcessor(), 
            new JetBrainsAnnotationsNotNullProcessor()
    );
    
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
     * @param v The Javaversion used to compile the project sources 
     */
    public void setJavaVersionCompliance(JavaVersion v) {
        int majorVersion = getMajorVersion(v);
        int maxVersion = Math.min(MAX_COMPATIBLE_JAVA_VERSION, majorVersion);

        if (maxVersion != majorVersion) {
            getProject().getLogger().warn(
                    "Project target is Java {} but source code transformation max supported version is {}", 
                    majorVersion, 
                    maxVersion
            );   
        }
        
        compliance = maxVersion;
        getProject().getLogger().debug("source code transformation uses Java {} compliance", maxVersion);
    }
    
    private static final int getMajorVersion(JavaVersion v) {
        return Integer.parseInt(v.getMajorVersion().replaceFirst("\\..*", ""));
    }
    
    @TaskAction
    void run() {
        Logger log = getProject().getLogger();
        
        // No source code to spoon.
        if (srcFolders.isEmpty()) {
            log.debug("cabe: no source folders");
            return;
        }

        Launcher launcher = new Launcher();

        srcFolders.forEach(launcher::addInputResource);
        launcher.setSourceOutputDirectory(outFolder.getAbsolutePath());
        
        Environment environment = launcher.getEnvironment();
        environment.setComplianceLevel(Math.min(compliance, MAX_COMPATIBLE_JAVA_VERSION));
        environment.setPreserveLineNumbers(true);
        environment.setOutputType(OutputType.COMPILATION_UNITS);
        environment.setAutoImports(true);

        List<String> classPathStrings = new ArrayList<>();
        classpath.forEach(p -> classPathStrings.add(p.toString()));
        environment.setSourceClasspath(classPathStrings.stream().toArray(String[]::new));

        PROCESSORS.forEach(processor -> {
            launcher.addProcessor(processor);
            processor.patchEnvironment(environment);
        });

        getProject().getLogger().debug("calling SPOON launcher");
        launcher.run();
    }

}
