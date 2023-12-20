package com.dua3.cabe.gradle;

import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.compile.JavaCompile;

import java.util.Objects;

/**
 * The Gradle plugin class for Cabe.
 */
@NonNullApi
public class CabeGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        Logger log = project.getLogger();

        // check that JavaPlugin is loaded
        if (!project.getPlugins().hasPlugin(JavaPlugin.class)) {
            throw new IllegalStateException("the java plugin is required for cabe to work");
        }

        // create extension
        CabeExtension extension = project.getExtensions().create("cabe", CabeExtension.class, project);

        // Adds task before the evaluation of the project to access of values
        // overloaded by the developer.
        project.afterEvaluate(p -> {
            // wire the cabe input task
            project.getTasks().create("cabe", CabeTask.class, t -> {
                assert t instanceof CabeTask;

                log.debug("initialising cabe task");

                // set directories
                t.getInputDirectory().set(extension.getInputDirectory());
                t.getOutputDirectory().set(extension.getOutputDirectory());
                t.getClassPath().set(extension.getClassPath());

                // run after the compileJava  task
                JavaCompile compileJavaTask = Objects.requireNonNull(
                        project.getTasks().withType(JavaCompile.class).findByName("compileJava"),
                        "task 'compileJava' not found"
                );
                compileJavaTask.finalizedBy(t);

                // change the compileJava class output directory to the cabe class input directory
                compileJavaTask.setDestinationDir(project.file(t.getInputDirectory().get()));

                // make the classes taks depend on the cabe task
                Task classesTask = Objects.requireNonNull(
                        project.getTasks().getByName("classes"),
                        "task 'classes' not found"
                );
                classesTask.dependsOn(t);
            });

        });
    }
}
