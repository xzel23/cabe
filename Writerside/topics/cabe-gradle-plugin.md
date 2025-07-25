# The Cabe Gradle Plugin

The Cabe Gradle Plugin is a plugin that integrates [Cabe](https://xzel23.github.io/cabe/cabe.html) bytecode instrumentation with Gradle projects. Cabe is a Java bytecode instrumentation tool that inserts runtime checks based on JSpecify annotations into your class files.

## Purpose and Benefits

Cabe helps implement the [Fail-Fast Principle](https://www.martinfowler.com/ieeeSoftware/failFast.pdf) by automatically adding runtime checks for annotated method parameters. This provides several benefits:

- **Early Detection**: Violations of nullability contracts are detected immediately at the point of violation
- **Improved Debugging**: Clear error messages that identify the exact parameter that violated the contract
- **Reduced Boilerplate**: No need to manually write null checks for annotated parameters
- **Consistent Enforcement**: Ensures that nullability contracts are enforced consistently across your codebase

## Requirements

- Java 17 or later
- Gradle 8 or later
- JSpecify annotations (typically org.jspecify:jspecify:1.0.0)

## Usage

### 1. Add the Plugin to Your Build Script

Add the Cabe Gradle Plugin to your project's `build.gradle.kts` file:

```kotlin
plugins {
    id("java")
    id("com.dua3.cabe") version "%PLUGIN_VERSION%"
}
```

Or in a traditional Groovy `build.gradle` file:

```groovy
plugins {
    id 'java'
    id 'com.dua3.cabe' version '%PLUGIN_VERSION%'
}
```

Alternatively, you can apply the plugin using the `apply` method:

```kotlin
apply(plugin = "com.dua3.cabe")
```

### 2. Add JSpecify Dependency

Add the JSpecify annotations to your project:

```kotlin
dependencies {
    implementation("org.jspecify:jspecify:1.0.0")
}
```

### 3. Configure the Plugin (Optional)

Configure the Cabe plugin using the `cabe` extension:

```kotlin
cabe {
    // Use com.dua3.cabe.processor.Configuration
    config.set(com.dua3.cabe.processor.Configuration.STANDARD) // or DEVELOPMENT, NO_CHECKS
    verbosity.set(1) // 0-3, where 0 is minimal logging and 3 is verbose
}
```

Or using the `configure` method:

```kotlin
configure<com.dua3.cabe.gradle.CabeExtension> {
    // Use com.dua3.cabe.processor.Configuration
    config.set(com.dua3.cabe.processor.Configuration.STANDARD)
    verbosity.set(1)
}
```

### 4. Use JSpecify Annotations in Your Code

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

The Cabe Gradle Plugin supports the following configuration options:

### config

Controls the configuration mode for Cabe processing.

```kotlin
cabe {
    // Use com.dua3.cabe.processor.Configuration
    config.set(com.dua3.cabe.processor.Configuration.STANDARD)
}
```

Possible values:
- **com.dua3.cabe.processor.Configuration.STANDARD**: Use standard assertions for private API methods, throw NullPointerException for public API methods (default)
- **com.dua3.cabe.processor.Configuration.DEVELOPMENT**: Failed checks will always throw an AssertionError, also checks return values
- **com.dua3.cabe.processor.Configuration.NO_CHECKS**: Do not add any null checks (class files are copied unchanged)
- **Custom configuration**: For advanced configuration (see Cabe documentation for details)

### verbosity

Controls the level of logging output.

```kotlin
cabe {
    verbosity.set(1)
}
```

Possible values:
- **0**: Show warnings and errors only (default)
- **1**: Show basic processing information
- **2**: Show detailed information
- **3**: Show all information

## Complete Example

Here's a complete example of a Gradle project using the Cabe Gradle Plugin:

```kotlin
plugins {
    id("java")
    id("application")
    id("com.dua3.cabe") version "%PLUGIN_VERSION%"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jspecify:jspecify:1.0.0")
}

cabe {
    // Use com.dua3.cabe.processor.Configuration.STANDARD
    config.set(com.dua3.cabe.processor.Configuration.STANDARD)
    verbosity.set(1)
}

application {
    mainClass.set("hello.Hello")
}

tasks.withType<JavaExec> {
    enableAssertions = true
}
```

## Advanced configuration

### Using different Configurations for Development and Release Builds

You can also automatically select a configuration based on your version string. In this example, strict checking is done
for snapshot and beta versions whereas a release build will use the standard configuration:

<tabs>
    <tab title="Kotlin DSL">
        <code-block lang="Kotlin">
            val isSnapshot = project.version.toString().toDefaultLowerCase().contains("snapshot")
            cabe {
                if (isSnapshot) {
                    config.set(Configuration.DEVELOPMENT)
                } else {
                    config.set(Configuration.STANDARD)
                }
            }
        </code-block>
    </tab>
    <!-- FIXME add Groovy syntax -->
</tabs>

### Defining Custom Configurations

You can define a custom configuration that differs from the provided predefined configurations by providing a
configuration String:

<code-block lang="Kotlin">
    cabe {
        config.set(Configuration.parse("publicApi=THROW_NPE:privateApi=ASSERT:returnValue=ASSERT_ALWAYS"))
    }
</code-block>

When using a configuration String, you can use either

- a predefined name: "STANDARD", "DEVELOPMEN", "NOCHECKSS"
- a single Check to be used public and private API and return values
- multiple combination of keys ("publicApi", "privateApi", "returnValue") and checks;
  in this the remaining will be set to "NO_CHECK"

Examples:

| Configuration String                     | Public API    | Private API   | Return Value  |
|------------------------------------------|---------------|---------------|---------------|
| "STANDARD"                               | THROW_NPE     | ASSERT        | NO_CHECK      |
| "DEVELOPMENT"                            | ASSERT_ALWAYS | ASSERT_ALWAYS | ASSERT_ALWAYS |
| "NO_CHECKS"                              | NO_CHECK      | NO_CHECK      | NO_CHECK      |
| "THROW_NPE"                              | THROW_NPE     | THROW_NPE     | THROW_NPE     |
| "ASSERT"                                 | ASSERT        | ASSERT        | ASSERT        |
| "ASSERT_ALWAYS"                          | ASSERT_ALWAYS | ASSERT_ALWAYS | ASSERT_ALWAYS |
| "NO_CHECK"                               | NO_CHECK      | NO_CHECK      | NO_CHECK      |
| "THROW_NPE"                              | THROW_NPE     | THROW_NPE     | THROW_NPE     |
| "publicApi=THROW_NPE"                    | THROW_NPE     | NO_CHECK      | NO_CHECK      |
| "publicApi=THROW_NPE:returnValue=ASSERT" | THROW_NPE     | NO_CHECK      | ASSERT        |
| "publicApi=THROW_IAE:privateApi=ASSERT"  | THROW_IAE     | ASSERT        | NO_CHECK      |

You can also use the standard record constructor of <code>Configuration</code>

<code-block lang="Kotlin">
    cabe {
        config.set(new Configuration(Check.THROW_NPE, Check.ASSERT, Check.ASSERT_ALWAYS))
    }
</code-block>

## Troubleshooting

### Common Issues

#### Class Files Not Being Processed

If your class files aren't being processed, check:

1. The Java plugin is applied before the Cabe plugin
2. The Cabe plugin is correctly applied to your project
3. The build output directory is correctly set

#### NoClassDefFoundError for JSpecify Annotations

If you get `NoClassDefFoundError` for JSpecify annotations at runtime, make sure:

1. The JSpecify dependency is correctly added to your project
2. The dependency scope is appropriate (usually `implementation`)
3. For modular projects, your module requires the JSpecify module

#### Unexpected NullPointerExceptions

If you're getting unexpected NullPointerExceptions:

1. Check that your code respects the nullability contracts specified by the annotations
2. Consider using a different configuration during development (e.g., `com.dua3.cabe.processor.Configuration.DEVELOPMENT`)
3. Increase the verbosity level to get more detailed information about the processing
4. Enable assertions when running your application:
   ```kotlin
   tasks.withType<JavaExec> {
       enableAssertions = true
   }
   ```

## Further Reading

- [JSpecify Project](https://jspecify.dev/)
- [Fail-Fast Principle](https://www.martinfowler.com/ieeeSoftware/failFast.pdf)