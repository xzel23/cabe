# The Cabe Maven Plugin

The Cabe Maven Plugin is a community-contributed plugin that integrates [Cabe](https://xzel23.github.io/cabe/cabe.html) bytecode instrumentation with Maven projects. Cabe is a Java bytecode instrumentation tool that inserts runtime checks based on JSpecify annotations into your class files.

## Purpose and Benefits

Cabe helps implement the [Fail-Fast Principle](https://www.martinfowler.com/ieeeSoftware/failFast.pdf) by automatically adding runtime checks for annotated method parameters. This provides several benefits:

- **Early Detection**: Violations of nullability contracts are detected immediately at the point of violation
- **Improved Debugging**: Clear error messages that identify the exact parameter that violated the contract
- **Reduced Boilerplate**: No need to manually write null checks for annotated parameters
- **Consistent Enforcement**: Ensures that nullability contracts are enforced consistently across your codebase

## Requirements

- Java 17 or later
- Maven 3.6.0 or later
- JSpecify annotations (typically org.jspecify:jspecify:1.0.0)

## Usage

### 1. Add the Plugin to Your POM

Add the Cabe Maven Plugin to your project's `pom.xml`:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.dua3.cabe</groupId>
      <artifactId>cabe-maven-plugin</artifactId>
      <version>%PLUGIN_VERSION%</version>
      <executions>
        <execution>
          <goals>
            <goal>cabe</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

### 2. Configure the Compiler to Use a Separate Output Directory

To avoid processing class files multiple times, configure the Maven Compiler Plugin to output compiled classes to a separate directory:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.13.0</version>
  <executions>
    <execution>
      <id>default-compile</id>
      <configuration>
        <compilerArguments>
          <d>${project.build.directory}/unprocessed-classes</d>
        </compilerArguments>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### 3. Configure the Cabe Plugin

Configure the Cabe plugin to process the unprocessed classes:

```xml
<plugin>
  <groupId>com.dua3.cabe</groupId>
  <artifactId>cabe-maven-plugin</artifactId>
  <version>%PLUGIN_VERSION%</version>
  <configuration>
    <inputDirectory>${project.build.directory}/unprocessed-classes</inputDirectory>
    <verbosity>1</verbosity>
    <configurationString>STANDARD</configurationString>
  </configuration>
  <executions>
    <execution>
      <goals>
        <goal>cabe</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

### 4. Add JSpecify Dependency

Add the JSpecify annotations to your project:

```xml
<dependencies>
  <dependency>
    <groupId>org.jspecify</groupId>
    <artifactId>jspecify</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

### 5. Use JSpecify Annotations in Your Code

Use JSpecify annotations in your code to specify nullability contracts:

```java
import org.jspecify.annotations.NonNull;

public class Hello {
    public static void main(String[] args) {
        sayHello("World"); // Works fine
        sayHello(null);    // Will throw NullPointerException at runtime
    }

    public static void sayHello(@NonNull String name) {
        System.out.println("Hello, " + name + "!");
    }
}
```

## Configuration Options

The Cabe Maven Plugin supports the following configuration options:

### verbosity

Controls the level of logging output.

```xml
<verbosity>1</verbosity>
```

Possible values:
- **0**: Show warnings and errors only (default)
- **1**: Show basic processing information
- **2**: Show detailed information
- **3**: Show all information

### inputDirectory

The directory containing the compiled class files to process.

```xml
<inputDirectory>${project.build.directory}/unprocessed-classes</inputDirectory>
```

Default: `${project.build.outputDirectory}`

### outputDirectory

The directory where processed class files will be written.

```xml
<outputDirectory>${project.build.outputDirectory}</outputDirectory>
```

Default: `${project.build.outputDirectory}`

### configurationString

The configuration mode for Cabe processing.

```xml
<configurationString>STANDARD</configurationString>
```

Possible values:
- **STANDARD**: Use standard assertions for private API methods, throw NullPointerException for public API methods (default)
- **DEVELOPMENT**: Failed checks will always throw an AssertionError, also checks return values
- **NO_CHECKS**: Do not add any null checks (class files are copied unchanged)
- **Custom configuration string**: For advanced configuration (see Cabe documentation for details)

## Complete Example

Here's a complete example of a Maven project using the Cabe Maven Plugin:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.example</groupId>
  <artifactId>hello-maven</artifactId>
  <version>1.0.0-SNAPSHOT</version>

  <properties>
    <maven.compiler.release>17</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.jspecify</groupId>
      <artifactId>jspecify</artifactId>
      <version>1.0.0</version>
    </dependency>
  </dependencies>

  <build>
    <finalName>hello-maven</finalName>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.13.0</version>
        <executions>
          <execution>
            <id>default-compile</id>
            <configuration>
              <compilerArguments>
                <d>${project.build.directory}/unprocessed-classes</d>
              </compilerArguments>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>com.dua3.cabe</groupId>
        <artifactId>cabe-maven-plugin</artifactId>
        <version>%PLUGIN_VERSION%</version>
        <configuration>
          <inputDirectory>${project.build.directory}/unprocessed-classes</inputDirectory>
          <verbosity>1</verbosity>
          <configurationString>STANDARD</configurationString>
        </configuration>
        <executions>
          <execution>
            <goals>
              <goal>cabe</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <version>3.4.2</version>
        <configuration>
          <archive>
            <manifest>
              <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
              <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
              <addClasspath>true</addClasspath>
              <mainClass>hello.Hello</mainClass>
            </manifest>
          </archive>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

## Troubleshooting

### Common Issues

#### Class Files Not Being Processed

If your class files aren't being processed, check:

1. The `inputDirectory` configuration is correct
2. The Maven Compiler Plugin is configured to output to the correct directory
3. The Cabe plugin is bound to the correct phase (default is `process-classes`)

#### NoClassDefFoundError for JSpecify Annotations

If you get `NoClassDefFoundError` for JSpecify annotations at runtime, make sure:

1. The JSpecify dependency is correctly added to your project
2. The dependency scope is appropriate (usually `compile`)

#### Unexpected NullPointerExceptions

If you're getting unexpected NullPointerExceptions:

1. Check that your code respects the nullability contracts specified by the annotations
2. Consider using a different `configurationString` during development (e.g., `DEVELOPMENT`)
3. Increase the `verbosity` level to get more detailed information about the processing

## Further Reading

- [JSpecify Project](https://jspecify.dev/)
- [Fail-Fast Principle](https://www.martinfowler.com/ieeeSoftware/failFast.pdf)
