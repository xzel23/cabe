Cabe [![MIT License](https://img.shields.io/badge/license-MIT-blue)](LICENSE) [![Language](https://img.shields.io/badge/language-Java-blue.svg?style=flat-square)](https://github.com/topics/java)
====

A bytecode instrumentation tool that inserts null checks based on JSpecify annotations.

## TLDR

Add this to the Gradle build file of your project that uses JSpecify annotations to add automatic null-checks to the public API of your project and assertion based
null checks to the private API:

```kotlin
plugins {
    id("com.dua3.cabe") version "3.3.0"
}
```

## Introduction

Cabe helps implement the [Fail-Fast Principle](https://www.martinfowler.com/ieeeSoftware/failFast.pdf) by automatically adding runtime checks for annotated method parameters. This provides several benefits:

- **Early Detection**: Violations of nullability contracts are detected immediately at the point of violation
- **Improved Debugging**: Clear error messages that identify the exact parameter that violated the contract
- **Reduced Boilerplate**: No need to manually write null checks for annotated parameters
- **Consistent Enforcement**: Ensures that nullability contracts are enforced consistently across your codebase

## Usage

### Gradle

1. Add the plugin to your build script:

```kotlin
plugins {
    id("java")
    id("com.dua3.cabe") version "3.3.0"
}
```

2. Add JSpecify dependency:

```kotlin
dependencies {
    implementation("org.jspecify:jspecify:1.0.0")
}
```

3. Add JSpecify annotations to your code.

### Maven

1. Add the plugin to your POM:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.dua3.cabe</groupId>
      <artifactId>cabe-maven-plugin</artifactId>
      <version>3.3.0</version>
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

2. Configure the compiler to use a separate output directory:

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

3. Configure the Cabe plugin:

```xml
<plugin>
  <groupId>com.dua3.cabe</groupId>
  <artifactId>cabe-maven-plugin</artifactId>
  <version>3.3.0</version>
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

4. Add JSpecify dependency:

```xml
<dependencies>
  <dependency>
    <groupId>org.jspecify</groupId>
    <artifactId>jspecify</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```
5. Add JSpecify annotations to your code.

## Building from Source

To build Cabe from source, use the provided `build.sh` script:

```bash
./build.sh
```

This script automatically:
1. Builds the project
2. Runs processor and plugin tests
3. Publishes artifacts to the local Maven repository
4. Tests the Gradle plugin
5. Builds example projects

The script ensures that all components work correctly together and is the recommended way to build the project.

## Documentation

For detailed documentation, including configuration options, advanced usage, and examples, please refer to:

- [Cabe Documentation](https://xzel23.github.io/cabe/cabe.html)
- [JSpecify Project](https://jspecify.dev/)
