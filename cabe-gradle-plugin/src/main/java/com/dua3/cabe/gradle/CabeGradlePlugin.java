package com.dua3.cabe.gradle;

import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.util.Optional;

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
            Optional<Task> compileJavaTask = p.getTasksByName("compileJava", true)
                    .stream()
                    .findFirst();

            project.getTasks().create("cabe", CabeTask.class,  t -> {
                log.debug("initialising cabe task");
                JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
                SourceSet mainSrc = javaExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

                t.srcFolders.addAll(mainSrc.getJava().getSrcDirs().stream().map(File::toString).toList());
                log.debug("cabe source folder(s) is {}", t.srcFolders);
                t.outFolder = project.file(project.getBuildDir().toPath().resolve("generated-sources").resolve("cabe"));
                log.debug("cabe output folder is {}", t.outFolder);
                
                compileJavaTask.ifPresent(cj -> {
                    log.debug("setting compileJava source folder to {}", t.outFolder);
                    JavaCompile jc = (JavaCompile) cj;
                    jc.setSource(t.outFolder);
    
                    log.debug("setting cabe classpath");
                    t.setClasspath(jc.getClasspath());
                    
                    log.debug("adding dependency on cabe to compileJava");
                    jc.dependsOn(t);
                });
            });
        });
    }
    
}
