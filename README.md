Cabe
====

From the [disambiguation page for Lombok](https://id.wikipedia.org/wiki/Lombok_(disambiguasi)) in the indonesian
Wikipedia:

___Lombok adalah nama lain dari cabai___ -- _Lombok is another name for cabai_.

This started out when I annotated one of my library projects
with [JetBrains annotations](https://github.com/JetBrains/java-annotations) and found out that while very helpful, this
doesn't help me at all in projects that use the library. I know that project Lombok provides similar annotations
to `@NotNull` that also add runtime checks, but I did not want to add runtime dependencies to my project.

When using `@NotNull` in IntelliJ IDEA, a runtime assertion is injected into the resulting bytecode, and I think that's
a cool thing to have:

- you can run your code with assertions enabled
- when you don't enable assertions, you have virtually no overhead
- static analysis tools can inspect the bytecode and infer which parameters are asserted not be null and output warnings
  where this contract is not fulfilled, even without relying on annotations to be present in the compiled library code

I started looking around to see how I could enable this in my Gradle build, but did not find any solution. I also did
not want to use Lombok because the Lombok assertions have runtime retention and add a runtime dependency to projects
that use it.

Also, Lombok tends to introduce new features that while innovative later are implemented in a different way in standard
JDK.

What I wanted is something more or less like Lombok, but closer to standard Java. Thus, the name was born: Cabai is
another name for Lombok, but on the island of Java, it is commonly called ***Cabe***!

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

Code
----

## cabe-annotations

This module defines custom annotations that are by Cabe:

- `NotNull` serves the same purpose as the `org.jetbrains.annotations.NotNull`, `javax.annotation.Nonnull` and other
  annotations. It can be used to specify that a method parameter must not be null. In contrast to the mentioned existing
  annotations, it is declared with a `SOURCE` retention policy, i.e., using this annotation does not introduce any
  runtime dependencies.

- `Nullable` marks a parameter as nullable.

- `NotNullApi` marks all parameters as `@NotNull` by default for an entire package or class.

- `NullableApi` marks all parameters as `@Nullable` by default for an entire package or class.

For each unannotated parameter, the annotations are checked on the declaring class where inner classes inherit
annotation from the class they are defined in. If no class level annotation is found, annotations from the package are
used.

**NOTE:** Use the `package-info.java` to annotate a package with `@NotNullApi`. Look at the
subproject `test-cabe-plugin` for examples.

## cabe-processor

This is the processor that injects assertions into the bytecode.

## cabe-gradle-plugin

This module contains the Gradle plugin that applies the processor to the class files.

## test-cabe-gradle-plugin

This module contains tests for the Gradle plugin.

## How to build

Run the shell script `build.sh` to build both packages and run tests. When everything succeeds, the script will
ask if you want to publish the plugin. Answer `n` unless you have updated the publishing coordinates and want to
publish the plugin.

Changes
-------

## Version 1.x

These versions worked on the source level, injecting code into the sources prior to compilation. This worked somewhat
but had the following issues:

- Support for features introduced in newer Java versions lagged behind because all new features had to be supported by
  the parser (SPOON) first.
- In constructors, the assertions could not be inserted before the call to super().
- It was not possible to add assertions for record fields.
- When debugging, sometimes line numbers were off and the debugger showed a message that source code and class file
  didn't match.

## Version 2.0

To solve the issues described above, I decided to do a full rewrite. I switched to working on the byte code instead
using Javassist instead of SPOON. This seemed easy at first but it turned out to be a little bit more complex
than I thought. First of all, I had to change the annotations classes because in Version 1.x annotations had SOURCE
retention and were not present in the byte code. That's why you now need version 2.0 of cabe-annotations to work
with the plugin. Also it proved to be much more complicated to get the mapping of parameters to the actual parameter
names correct. I added lots of test cases to make sure everything works correctly now.

So these are the main changes:

- Annotations now have CLASS retention.
- In derived classes, assertions are checked before super is called.
- Annotations on record parameters work.
- Full support for Java 21.
- The debugging issues are solved.

## Outlook

Next on my list are these features (in no specific order):

- Add support for annotations on return values.
- Make the plugin configurable, for example to use Objects.requireNonNull() for public and assertions for nun public
  APIs.
- Support different annotations like JetBrains or JSpecify. The reason I currently use my own annotations library is
  that I wanted a package wide annotation, @NotNullAPI. While for example JSpecify has something comparable, it also
  has annotations on modules and return values, and that is not yet supported.
