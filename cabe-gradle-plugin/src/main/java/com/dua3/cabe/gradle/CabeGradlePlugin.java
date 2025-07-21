package com.dua3.cabe.gradle;

import com.dua3.cabe.processor.Configuration;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.util.Objects;

/**
 * The Gradle plugin class for Cabe.
 */
public class CabeGradlePlugin implements Plugin<Project> {

    /**
     * Constructs a new instance of the CabeGradlePlugin.
     */
    public CabeGradlePlugin() {
        // nothing to do
    }

    @Override
    public void apply(Project project) {
        Logger log = project.getLogger();

        // check that JavaPlugin is loaded
        if (!project.getPlugins().hasPlugin(JavaPlugin.class)) {
            throw new IllegalStateException("the java plugin is required for cabe to work");
        }

        // create extension
        CabeExtension extension = project.getExtensions().create("cabe", CabeExtension.class, project);

        // Add the task before the evaluation of the project to allow access to values overloaded by the developer.
        project.afterEvaluate(p -> {
            log.info("plugin {} applied successfully", getClass().getSimpleName());

            // get the compileJava task
            JavaCompile compileJavaTask = Objects.requireNonNull(
                    project.getTasks().withType(JavaCompile.class).findByName("compileJava"),
                    "task 'compileJava' not found"
            );

            // Get the original output directory (where compiled classes will be)
            File originalOutputDir = compileJavaTask.getDestinationDirectory().getAsFile().get();
            log.warn("Original output directory: {}", originalOutputDir);

            // Create a specific input directory for the cabe task
            File cabeInputDir = project.getLayout().getBuildDirectory().dir("classes-cabe-input").get().getAsFile();
            log.warn("Cabe input directory: {}", cabeInputDir);
            
            // Ensure the input directory exists
            cabeInputDir.mkdirs();
            log.warn("Created input directory: {}", cabeInputDir);

            // Set the extension's input and output directories
            extension.getInputDirectory().set(cabeInputDir);
            extension.getOutputDirectory().set(originalOutputDir);
            log.warn("Extension input directory set to: {}", extension.getInputDirectory().get().getAsFile());
            log.warn("Extension output directory set to: {}", extension.getOutputDirectory().get().getAsFile());

            // Create a copy task to copy compiled class files to the input directory
            Copy copyClassesTask = project.getTasks().create("copyClassesForCabe", Copy.class, copy -> {
                copy.from(originalOutputDir);
                copy.into(cabeInputDir);
                copy.include("**/*.class");
                copy.dependsOn(compileJavaTask);
                log.warn("Created copy task to copy class files from {} to {}", originalOutputDir, cabeInputDir);
            });

            // prepare the classpaths
            org.gradle.api.artifacts.Configuration compileClasspath = p.getConfigurations().getByName("compileClasspath");
            org.gradle.api.artifacts.Configuration runtimeClasspath = p.getConfigurations().getByName("runtimeClasspath");

            // wire the cabe input task
            CabeTask cabeTask = project.getTasks().create("cabe", CabeTask.class, t -> {
                log.debug("initialising cabe task");

                // set verbosity level
                t.getVerbosity().set(extension.getVerbosity());

                // set the configuration
                t.getConfig().set(extension.getConfig().getOrElse(Configuration.STANDARD));

                // set directories
                t.getInputDirectory().set(extension.getInputDirectory());
                t.getOutputDirectory().set(extension.getOutputDirectory());
                t.getClassPath().set(compileClasspath);
                t.getRuntimeClassPath().set(runtimeClasspath);
                t.getJavaExecutable().set(compileJavaTask.getJavaCompiler().get().getExecutablePath().getAsFile().toPath().getParent().resolve("java").toString());
                
                // Make the cabe task depend on the copy task
                t.dependsOn(copyClassesTask);
                log.warn("Made cabe task depend on copy task");
            });
            
            // run the cabe task after the compileJava task
            compileJavaTask.finalizedBy(cabeTask);
            log.warn("Made compileJava task finalized by cabe task");

            // make the classes task depend on the cabe task
            Task classesTask = Objects.requireNonNull(
                    project.getTasks().getByName("classes"),
                    "task 'classes' not found"
            );
            classesTask.dependsOn(cabeTask);
            log.warn("Made classes task depend on cabe task");
        });
    }
}
