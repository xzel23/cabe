package com.dua3.cabe.maven;

import com.dua3.cabe.processor.ClassPatcher;
import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Cabe Maven goal definition
 */
@Mojo(name = "cabe", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class CabeMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;
  /**
   * The verbosity level.
   * <ul>
   *   <li> <b>0</b> - show warnings and errors only (default)
   *   <li> <b>1</b> - show basic processing information
   *   <li> <b>2</b> - show detailed information
   *   <li> <b>3</b> - show all information
   * </ul>
   */
  @Parameter(property = "cabe.verbosity")
  private Integer verbosity;
  /**
   * The input directory for the Cabe processing
   */
  @Parameter(property = "cabe.inputDirectory", defaultValue = "${project.build.outputDirectory}")
  private Path inputDirectory;
  /**
   * The output directory for the Cabe processing
   */
  @Parameter(property = "cabe.outputDirectory", defaultValue = "${project.build.outputDirectory}")
  public Path outputDirectory;
  /**
   * The configuration string for the Cabe
   * <ul>
   *  <li> <b>STANDARD</b> - use standard assertions for private API methods, throw NullPointerException for public API methods
   *  <li> <b>DEVELOPMENT</b> - failed checks will always throw an AssertionError, also checks return values
   *  <li> <b>NO_CHECKS</b> - do not add any null checks (class files are copied unchanged)
   *  <li> &lt;configstr&gt; - custom configuration string, please check documentation for details
   * </ul>
   */
  @Parameter(property = "cabe.configurationString", defaultValue = "STANDARD")
  public String configurationString;

  /**
   * Default constructor
   */
  public CabeMojo() {
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      String jarLocation = Paths.get(
              ClassPatcher.class.getProtectionDomain().getCodeSource().getLocation().toURI())
          .toString();
      String systemClassPath = System.getProperty("java.class.path");

      String classpath = project.getArtifacts().stream()
          .map(Artifact::getFile)
          .map(File::toString)
          .distinct()
          .collect(Collectors.joining(File.pathSeparator));

      String javaExec = Path.of(System.getProperty("java.home"), "bin", "java").toString();
      getLog().info("Java executable: %s".formatted(javaExec));

      int v = Objects.requireNonNullElse(verbosity, 0);
      String[] args = {
          javaExec,
          "-classpath", systemClassPath,
          "-jar", jarLocation,
          "-i", inputDirectory.toString(),
          "-o", outputDirectory.toString(),
          "-c", configurationString,
          "-cp", classpath,
          "-v", Integer.toString(v)
      };

      if (v > 0) {
        getLog().debug("Instrumenting class files: %s".formatted(String.join(" ", args)));
      }

      getLog().info(String.join(" ", args));
      ProcessBuilder pb = new ProcessBuilder(args);

      Process process = pb.start();

      try (CopyOutput copyStdErr = new CopyOutput(process.errorReader(), System.err::println);
          CopyOutput ignored = new CopyOutput(process.inputReader(),
              v > 1 ? System.out::println : s -> {
              })) {
        int exitCode = process.waitFor();
        if (exitCode != 0) {
          throw new MojoFailureException("Instrumenting class files failed\n\n" + copyStdErr);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      throw new MojoFailureException(
          "An error occurred while instrumenting classes: " + e.getMessage(), e);
    }
  }

  /**
   * This class is responsible for copying the output of a Reader to a specified Consumer. The first
   * 10 lines are stored.
   */
  private class CopyOutput implements AutoCloseable {

    public static final int MAX_LINES = 10;
    Thread thread;
    List<String> firstLines = new ArrayList<>();

    CopyOutput(Reader reader, Consumer<String> printer) {
      thread = new Thread(() -> {
        try (BufferedReader r = new BufferedReader(reader)) {
          String line;
          while ((line = r.readLine()) != null) {
            printer.accept(line);
            if (firstLines.size() < MAX_LINES) {
              firstLines.add(line);
            } else if (firstLines.size() == MAX_LINES) {
              firstLines.add("...");
            }
          }
        } catch (Exception e) {
          getLog().warn("exception reading ClassPatcher error output");
        }
      });
      thread.start();
    }

    @Override
    public void close() {
      try {
        thread.join(5000); // Wait 5000ms for the thread to die.
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      if (thread.isAlive()) {
        getLog().warn("output thread did not stop");
        thread.interrupt();
      }
    }

    @Override
    public String toString() {
      return String.join("\n", firstLines);
    }
  }

}
