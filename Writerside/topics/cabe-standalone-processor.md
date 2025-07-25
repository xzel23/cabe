# The Cabe Standalone Processor

The Cabe Standalone Processor is a command-line tool that integrates [Cabe](https://xzel23.github.io/cabe/cabe.html) bytecode instrumentation directly with your class files. Cabe is a Java bytecode instrumentation tool that inserts runtime checks based on JSpecify annotations into your class files.

## Purpose and Benefits

Cabe helps implement the [Fail-Fast Principle](https://www.martinfowler.com/ieeeSoftware/failFast.pdf) by automatically adding runtime checks for annotated method parameters. This provides several benefits:

- **Early Detection**: Violations of nullability contracts are detected immediately at the point of violation
- **Improved Debugging**: Clear error messages that identify the exact parameter that violated the contract
- **Reduced Boilerplate**: No need to manually write null checks for annotated parameters
- **Consistent Enforcement**: Ensures that nullability contracts are enforced consistently across your codebase

## Requirements

- Java 17 or later
- JSpecify annotations (typically org.jspecify:jspecify:1.0.0) in your compiled classes
- The Cabe processor JAR file (cabe-processor-all.jar)

## Downloading the Processor

You can download the Cabe processor JAR file from the Maven Central Repository:

```bash
curl https://repo1.maven.org/maven2/com/dua3/cabe/cabe-processor-all/%PROCESSOR_VERSION%/cabe-processor-all-%PROCESSOR_VERSION%
jar -o cabe-processor-all.jar
```

Replace `%PROCESSOR_VERSION%` with the version you want to use.

## Command-Line Usage

The basic syntax for using the Cabe processor is:

```bash
java -jar cabe-processor-all.jar -i <input-folder> -o <output-folder> [-c <configuration>] [-cp <classpath>] [-v <verbosity>]
```

### Required Parameters

- `-i <input-folder>`: The directory containing the compiled class files to process
- `-o <output-folder>`: The directory where processed class files will be written

### Optional Parameters

- `-c <configuration>`: The configuration mode for Cabe processing (default: STANDARD)
- `-cp <classpath>`: The classpath for resolving classes
- `-v <verbosity>`: The verbosity level (0-3, default: 0)
- `--help`: Display help information

## Configuration Options

The Cabe processor supports several configuration modes that control how null checks are generated:

### Predefined Configurations

- **STANDARD**: Use standard assertions for private API methods, throw NullPointerException for public API methods (default)
- **DEVELOPMENT**: Failed checks will always throw an AssertionError, also checks return values
- **NO_CHECKS**: Do not add any null checks (class files are copied unchanged)

### Custom Configurations

You can define a custom configuration using a configuration string with the following format:

```
publicApi=<check>:privateApi=<check>:returnValue=<check>
```

Where `<check>` can be one of:

- **NO_CHECK**: No checks are generated
- **ASSERT**: Standard Java assertions (controlled by -ea/-da JVM flags)
- **THROW_NPE**: Throws NullPointerException
- **ASSERT_ALWAYS**: Throws AssertionError regardless of JVM assertion settings
- **THROW_IAE**: Throws IllegalArgumentException

You can also specify a single check to be used for all types:

```
<check>
```

For example:
- `THROW_NPE` - Use NullPointerException for all checks
- `publicApi=THROW_NPE:privateApi=ASSERT` - Use NullPointerException for public API and assertions for private API

### Configuration Examples

| Configuration String                     | Public API    | Private API   | Return Value  |
|------------------------------------------|---------------|---------------|---------------|
| "STANDARD"                               | THROW_NPE     | ASSERT        | NO_CHECK      |
| "DEVELOPMENT"                            | ASSERT_ALWAYS | ASSERT_ALWAYS | ASSERT_ALWAYS |
| "NO_CHECKS"                              | NO_CHECK      | NO_CHECK      | NO_CHECK      |
| "THROW_NPE"                              | THROW_NPE     | THROW_NPE     | THROW_NPE     |
| "ASSERT"                                 | ASSERT        | ASSERT        | ASSERT        |
| "publicApi=THROW_NPE"                    | THROW_NPE     | NO_CHECK      | NO_CHECK      |
| "publicApi=THROW_NPE:returnValue=ASSERT" | THROW_NPE     | NO_CHECK      | ASSERT        |
| "publicApi=THROW_IAE:privateApi=ASSERT"  | THROW_IAE     | ASSERT        | NO_CHECK      |

## Verbosity Levels

The `-v` parameter controls the amount of information displayed during processing:

- **0**: Show warnings and errors only (default)
- **1**: Show basic processing information
- **2**: Show detailed information
- **3**: Show all information

## Usage Examples

### Basic Usage

Process class files from the `build/classes` directory and write the processed files to the same directory:

```bash
java -jar cabe-processor-all.jar -i build/classes -o build/classes
```

### Using a Different Configuration

Process class files using the DEVELOPMENT configuration:

```bash
java -jar cabe-processor-all.jar -i build/classes -o build/classes -c DEVELOPMENT
```

### Using a Custom Configuration

Process class files using a custom configuration:

```bash
java -jar cabe-processor-all.jar -i build/classes -o build/classes -c "publicApi=THROW_IAE:privateApi=ASSERT"
```

### Specifying a Classpath

Process class files with a specific classpath:

```bash
java -jar cabe-processor-all.jar -i build/classes -o build/classes -cp "lib/dependency1.jar:lib/dependency2.jar"
```

### Increasing Verbosity

Process class files with increased verbosity:

```bash
java -jar cabe-processor-all.jar -i build/classes -o build/classes -v 2
```

## Integration with Build Systems

While the standalone processor can be used directly, it's often more convenient to use the Cabe Maven or Gradle plugins for integration with build systems:

- [Cabe Maven Plugin](cabe-maven-plugin.md)
- [Cabe Gradle Plugin](cabe-gradle-plugin.md)

## Troubleshooting

### Common Issues

#### Class Files Not Being Processed

If your class files aren't being processed, check:

1. The input directory exists and contains class files
2. You have the correct permissions to read from the input directory and write to the output directory
3. The classpath includes all necessary dependencies

#### NoClassDefFoundError for JSpecify Annotations

If you get `NoClassDefFoundError` for JSpecify annotations at runtime, make sure:

1. The JSpecify dependency is correctly included in your runtime classpath
2. The processed class files are being used at runtime

#### Unexpected NullPointerExceptions

If you're getting unexpected NullPointerExceptions:

1. Check that your code respects the nullability contracts specified by the annotations
2. Consider using a different configuration during development (e.g., `DEVELOPMENT`)
3. Increase the verbosity level to get more detailed information about the processing

#### OutOfMemoryError

If you encounter an OutOfMemoryError, try increasing the Java heap size:

```bash
java -Xmx1g -jar cabe-processor-all.jar -i build/classes -o build/classes
```

## Further Reading

- [JSpecify Project](https://jspecify.dev/)
- [Fail-Fast Principle](https://www.martinfowler.com/ieeeSoftware/failFast.pdf)
