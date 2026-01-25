package com.dua3.cabe.gradle;

import com.dua3.cabe.processor.ClassPatcher;
import com.dua3.cabe.processor.Configuration;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * The CabeTask class is responsible for instrumenting class files using Cabe.
 */
@CacheableTask
public abstract class CabeTask extends DefaultTask {

    /**
     * Retrieves the configuration property for the Cabe plugin.
     *
     * @return the configuration property as a Property object of type Configuration
     */
    @Input
    public abstract Property<Configuration> getConfig();

    /**
     * Retrieves the verbosity property for the Cabe plugin.
     *
     * @return the verbosity property as a Property object of type Integer
     */
    @Input
    public abstract Property<Integer> getVerbosity();

    /**
     * Retrieves the input directory containing the class files to be instrumented.
     *
     * @return the input directory as a DirectoryProperty object
     */
    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getInputDirectory();

    /**
     * Retrieves the output directory where the instrumented class files will be stored.
     *
     * @return the output directory as a DirectoryProperty object
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    /**
     * Retrieves the classpath used for instrumentation.
     *
     * @return the classpath as a ConfigurableFileCollection object
     */
    @InputFiles
    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    /**
     * Retrieves the Java executable path.
     *
     * @return the Java executable path as a RegularFileProperty object
     */
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getJavaExecutable();

    /**
     * Retrieves the ExecOperations instance.
     *
     * @return the ExecOperations instance
     */
    @Inject
    protected abstract ExecOperations getExecOperations();

    /**
     * Instrument the class files.
     */
    @TaskAction
    public void instrument() {
        if (!getInputDirectory().isPresent() || !getInputDirectory().get().getAsFile().exists()) {
            getLogger().info("Cabe: input directory does not exist, skipping instrumentation");
            return;
        }

        Logger logger = getLogger();
        File inputDir = getInputDirectory().getAsFile().get();
        File outputDir = getOutputDirectory().getAsFile().get();

        logger.info("instrumenting classes using Cabe\n  inputDir : {}  outputDir: {}",
                inputDir.getAbsolutePath(),
                outputDir.getAbsolutePath()
        );

        try {
            // process files
            String jarLocation = Paths.get(ClassPatcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();

            String javaExecPath = getJavaExecutable().getAsFile().get().getAbsolutePath();
            if (javaExecPath.endsWith("/javac")) {
                javaExecPath = javaExecPath.substring(0, javaExecPath.length() - 5) + "java";
            } else if (javaExecPath.endsWith("\\javac.exe")) {
                javaExecPath = javaExecPath.substring(0, javaExecPath.length() - 10) + "java.exe";
            }
            final String javaExec = javaExecPath;
            logger.debug("Java executable: {}", javaExec);

            String cp = getClasspath().getFiles().stream()
                    .map(File::toString)
                    .distinct()
                    .collect(Collectors.joining(File.pathSeparator));

            int v = getVerbosity().getOrElse(0);
            getExecOperations().javaexec(spec -> {
                spec.setExecutable(javaExec);
                spec.getMainClass().set("-jar");
                spec.setArgs(java.util.List.of(
                        jarLocation,
                        "-i", inputDir.toString(),
                        "-o", outputDir.toString(),
                        "-c", getConfig().getOrElse(Configuration.STANDARD).getConfigString(),
                        "-cp", cp,
                        "-v", Integer.toString(v)
                ));
            });

        } catch (Exception e) {
            throw new GradleException("An error occurred while instrumenting classes: " + e.getMessage(), e);
        }
    }
}
