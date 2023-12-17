package com.dua3.cabe.gradle;

import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.compile.JavaCompile;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The gradle plugin class for Cabe.
 */
@NonNullApi
public class CabeGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        Logger log = project.getLogger();

        // check that JavaPlugin is loaded
        if (!project.getPlugins().hasPlugin(JavaPlugin.class)) {
            throw new IllegalStateException("java plugin required");
        }

        // create extension
        project.getExtensions().create("cabe", CabeExtension.class);

        // Adds task before the evaluation of the project to access of values
        // overloaded by the developer.
        project.afterEvaluate(p -> {
            List<Task> compileJavaTask = p.getTasksByName("compileJava", false)
                    .stream()
                    .collect(Collectors.toUnmodifiableList());

            project.getTasks().create("cabe", CabeTask.class, t -> {
                log.debug("initialising cabe task");

                compileJavaTask.forEach(cj -> {
                    JavaCompile jc = (JavaCompile) cj;
                    log.debug("preparing cabe task to run after compileJava: {}", jc);

                    Path classFolder = jc.getDestinationDirectory().getAsFile().get().toPath();
                    t.setClassFolder(classFolder);
                    log.debug("classes folder for cabe task: {}", classFolder);

                    log.debug("configuring cabe task to run after classes");
                    Task classesTask = project.getTasks().getByName("classes");
                    classesTask.finalizedBy(t);
                });
            });

        });
    }

}
