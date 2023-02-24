Cabe
====

From the [disambiguation page for Lombok](https://id.wikipedia.org/wiki/Lombok_(disambiguasi)) in the indonesian Wikipedia:

___Lombok adalah nama lain dari cabai___ -- _Lombok is another name for cabai_.

This started out when I annotated one of my library projects with [JetBrains annotations](https://github.com/JetBrains/java-annotations) and found out that while very helpful, this doesn't help me at all in projects that use the library. I know that project Lombok provides similar annotations to `@NotNull` that also add runtime checks, but I did not want to add runtime dependencies to my project.

When using `@NotNull` in IntelliJ IDEA, a runtime assertion is injected into the resulting bytecode, and I think that's a cool thing to have:

 - you can run your code with assertions enabled
 - when you don't enable assertions, you have virtually no overhead
 - static analysis tools can inspect the bytecode and infer which parameters are asserted not be null and output warnings where this contract is not fulfilled, even without relying on annotations to be present in the compiled library code 

I started looking around to see how I could enable this in my gradle build, but did not find any solution. I also did not want to use Lombok because the Lombok assertions have runtime rentention and add a runtime dependency to projects that use it. 

Also Lombok tends to introduce new features that while innovative later are implemented in a different way in standard JDK.

What I wanted is something more or less like Lombok, but closer to standard Java. Thus the name was born: Cabai is another name for Lombok, but on the island of Java, it is commonly called ***Cabe***!

Usage
-----

 - Add the plugin in `build.gradle`:
    ```
    plugins {
        id 'com.dua3.cabe
    }
    ```

 - Add a compile time dependency on cabe-annotations:   
    ```
    dependencies {
        compileOnly "com.dua3.cabe:cabe-annotations:<version>"
    }
    ```

 - Use annotations in your code:
   ```
   public void foo(@NotNull Bar bar) {
       ...
   }
   ```
   will be compiled to the equivalent of this (see below for a list of the supported annotations):
   ```
   public void foo(Bar bar) {
       assert ( bar != null, "parameter 'bar' must not be null" );
       ...
   }
   ```

Issues
------

- For technical reasons, parameters to constructor calls cannot be checked before the super constructor is run. This may lead to spotbugs complaining about unnecessary null checks.
- When building cabe under Windows, make sure the TERM variable is set correctly for your system or text output will look garbled. For Git Bash, use `export TERM=cygwin`.

Code
----

## cabe-annotations

This module defines custom annotations that can be processed by Cabe:

- `NotNull` serves the same purpose as the `org.jetbrains.annotations.NotNull`, `javax.annotation.Nonnull` and other annotations. It mcan be used to specify that a method parameter must not be null. In contrast to the mentioned existing annotations, it is declared with a `SOURCE` rentention policy, i. e. using this annotation does not introduce any runtime dependencies.

- `Nullable` marks a parameter as nullable.

- `NotNullApi` marks all parameters as `@NotNull` by default for an entire package or class.

- `NullableApi` marks all parameters as `@Nullable` by default for an entire package or class.

For each unannotated parameter, the annotations are checked on the declaring class where inner classes inherit annotation from the class they are defined in. If no class lavel annotation is found, annotations from the package are used. 

**NOTE:** Use the `package-info.file` to annotate a package with `@NotNullApi`. Look at the sub-project `test-cabe-plugin` for examples.

## cabe-gradle-plugin

This module contains the Gradle plugin used to process annotations.

## test-cabe-gradle-plugin

This module contains tests for the Gradle plugin.
