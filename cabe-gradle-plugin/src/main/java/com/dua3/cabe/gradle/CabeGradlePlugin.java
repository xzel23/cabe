package com.dua3.cabe.gradle;

import com.dua3.cabe.processor.Configuration;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.compile.JavaCompile;

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


            // wire the cabe input task using register (avoiding deprecated create)
            project.getTasks().register("cabe", CabeTask.class, t -> {
                log.debug("initialising cabe task");

                // set verbosity level
                t.getVerbosity().set(extension.getVerbosity());

                // set the configuration
                t.getConfig().set(extension.getConfig().getOrElse(Configuration.STANDARD));

                // Create providers for input and output directories
                DirectoryProperty originalClassesDirProvider = extension.getOriginalClassesDirectory();
                originalClassesDirProvider.set(project.getLayout().getBuildDirectory().dir("classes-cabe-input"));

                DirectoryProperty classesDirProvider = extension.getClassesDirectory();
                classesDirProvider.set(compileJavaTask.getDestinationDirectory());

                // prepare the classpaths
                org.gradle.api.artifacts.Configuration compileClasspath = p.getConfigurations().getByName("compileClasspath");
                org.gradle.api.artifacts.Configuration runtimeClasspath = p.getConfigurations().getByName("runtimeClasspath");

                // Set the CabeTask directories using providers
                t.getOriginalClassesDir().set(originalClassesDirProvider);
                t.getClassesDir().set(classesDirProvider);
                t.getClassPath().set(compileClasspath);
                t.getRuntimeClassPath().set(runtimeClasspath);
                t.getJavaExecutable().set(compileJavaTask.getJavaCompiler().get().getExecutablePath().getAsFile().toPath().getParent().resolve("java").toString());
            });

            // Set up task dependencies
            // These must happen eagerly at configuration time
            compileJavaTask.finalizedBy("cabe");
            
            // make the classes task depend on the cabe task
            Task classesTask = Objects.requireNonNull(
                    project.getTasks().getByName("classes"),
                    "task 'classes' not found"
            );
            classesTask.dependsOn("cabe");

        });
    }
}
