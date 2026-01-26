package com.dua3.cabe.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.compile.JavaCompile;

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

        // Configures instrumentation task per Java source set
        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
            JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);

            javaExtension.getSourceSets().configureEach(sourceSet -> {
                // Check if the source set has any Java source files
                if (sourceSet.getJava().isEmpty()) {
                    return;
                }

                String taskName = sourceSet.getTaskName("cabe", "");
                String compileJavaTaskName = sourceSet.getCompileJavaTaskName();

                // register the cabe task
                var cabeTaskProvider = project.getTasks().register(taskName, CabeTask.class, cabeTask -> {
                    cabeTask.setGroup("build");
                    cabeTask.setDescription("Instrument class files with Cabe for " + sourceSet.getName());

                    cabeTask.getConfig().set(extension.getConfig());
                    cabeTask.getVerbosity().set(extension.getVerbosity());

                    // Set input directory to compileJava's destination directory
                    var compileJavaTaskProvider = project.getTasks().named(compileJavaTaskName, JavaCompile.class);
                    cabeTask.getInputDirectory().set(compileJavaTaskProvider.flatMap(JavaCompile::getDestinationDirectory));

                    // Set output directory
                    cabeTask.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir("classes-cabe/" + sourceSet.getName()));

                    // Set classpath
                    cabeTask.getClasspath().from(sourceSet.getCompileClasspath());
                    cabeTask.getClasspath().from(sourceSet.getRuntimeClasspath());

                    // Set Java executable
                    cabeTask.getJavaExecutable().set(compileJavaTaskProvider.flatMap(c -> c.getJavaCompiler().map(jc -> jc.getExecutablePath())));

                    cabeTask.dependsOn(compileJavaTaskProvider);
                });

                // Wire instrumented classes to Jar tasks
                if (SourceSet.isMain(sourceSet)) {
                    project.getTasks().withType(Jar.class).configureEach(jarTask -> {
                        jarTask.from(cabeTaskProvider.map(CabeTask::getOutputDirectory));
                        // Exclude original classes from compileJava as they are now replaced by instrumented ones
                        var compileJavaTaskProvider = project.getTasks().named(compileJavaTaskName, JavaCompile.class);
                        var compileJavaOutputDirProvider = compileJavaTaskProvider.flatMap(JavaCompile::getDestinationDirectory);
                        jarTask.exclude(element -> {
                            var outputDir = compileJavaOutputDirProvider.getOrNull();
                            return outputDir != null && element.getFile().getAbsolutePath().startsWith(outputDir.getAsFile().getAbsolutePath());
                        });
                    });
                }

                // Wire instrumented classes to Test tasks
                project.getTasks().withType(Test.class).configureEach(testTask -> {
                    // This is a bit tricky, we need to ensure the instrumented classes are on the classpath
                    // instead of the original ones.
                    var compileJavaTaskProvider = project.getTasks().named(compileJavaTaskName, JavaCompile.class);
                    var compileJavaOutputDirProvider = compileJavaTaskProvider.flatMap(JavaCompile::getDestinationDirectory);

                    var originalClasspath = testTask.getClasspath();
                    var instrumentedClasses = cabeTaskProvider.map(CabeTask::getOutputDirectory);
                    testTask.setClasspath(project.files(instrumentedClasses, originalClasspath.filter(file -> {
                        var outputDir = compileJavaOutputDirProvider.getOrNull();
                        return outputDir == null || !file.equals(outputDir.getAsFile());
                    })));
                });

                // Wire instrumented classes to JavaExec tasks (like 'run' from application plugin)
                if (SourceSet.isMain(sourceSet)) {
                    project.getTasks().withType(JavaExec.class).configureEach(javaExecTask -> {
                        var compileJavaTaskProvider = project.getTasks().named(compileJavaTaskName, JavaCompile.class);
                        var compileJavaOutputDirProvider = compileJavaTaskProvider.flatMap(JavaCompile::getDestinationDirectory);

                        var originalClasspath = javaExecTask.getClasspath();
                        var instrumentedClasses = cabeTaskProvider.map(CabeTask::getOutputDirectory);
                        javaExecTask.setClasspath(project.files(instrumentedClasses, originalClasspath.filter(file -> {
                            var outputDir = compileJavaOutputDirProvider.getOrNull();
                            return outputDir == null || !file.equals(outputDir.getAsFile());
                        })));
                    });
                }
            });
        });
    }
}
