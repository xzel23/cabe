Cabe
====

From the [disambiguation page for Lombok](https://id.wikipedia.org/wiki/Lombok_(disambiguasi)) in the indonesian
Wikipedia:

___Lombok adalah nama lain dari cabai___ -- _Lombok is another name for cabai_.

This started out when I annotated one of my library projects
with [JetBrains annotations](https://github.com/JetBrains/java-annotations) and found out that while very helpful, this
doesn't help me at all in projects that use the library. I know that project Lombok provides similar annotations
to `@NonNull` that also add runtime checks, but I did not want to add runtime dependencies to my project.

When using `@NonNull` in IntelliJ IDEA, a runtime assertion is injected into the resulting bytecode, and I think that's
a cool thing to have:

- you can run your code with assertions enabled
- when you don't enable assertions, you have virtually no overhead
- static analysis tools can inspect the bytecode and infer which parameters are asserted not be null and output warnings
  where this contract is not fulfilled, even without relying on annotations to be present in the compiled library code

I started looking around to see how I could enable this in my Gradle build, but did not find any solution. I also did
not want to use Lombok — there's a big controversy about Lombok in the Java community that I will not comment on. My
personal reason not to use Lombok is that it introduced many things that plain Java in newer versions does out of the
box, but differently (i.e., Java records vs. Lombok records). I want my code to use standard Java wherever possible,
that's all.

What I wanted is something more or less like Lombok, but closer to standard Java. Thus, the name was born: Cabai is
another name for Lombok, but on the island of Java, it is commonly called ***Cabe***.

Usage
-----

- Add the plugin in `build.gradle`:
   ```
   plugins {
       id 'com.dua3.cabe
   }
   ```

- Add a dependency on cabe-annotations:
   ```
   dependencies {
       implementation "com.dua3.cabe:cabe-annotations:2.0"
   }
   ```

- Use annotations in your code:
  ```
  public void foo(@NonNull Bar bar) {
      ...
  }
  ```

  ... will be compiled to the equivalent of this (see below for a list of the supported annotations):

  ```
  public void foo(Bar bar) {
      assert bar != null : " bar is null";
      ...
  }
  ```
- in order to see the original parameter names as present in the source code, compile with the `-parameters` flag.
  If parameters are not present, error messages will use `arg<n>` where `n` is the position in the argument list, 
  starting with 1.

Plugin configuration
--------------------

The plugin provides a Gradle extension named "cabe."
You can use it to control the kind of null checks that are
generated.

There are three different predefined configurations:

- STANDARD: uses standard assertions for private and throws NullPointerException for public API method parameters.
  Assertions for privat API parameters can be controlled using the standard `-ea` and `-da` JVM flags.

- DEVELOPMENT: Always throws AssertionError for a failed check in both public and private API. These cannot be disabled
  using the `-da` JVM flag.

- NO_CHECKS: No checks are generated for both public and private API.

**Example for a custom configuration**

This example uses the Gradle Kotlin DSL:

```
    plugins {
      id("com.dua3.cabe") version "2.1-rc5"
    }

    // is it a snapshot or a release version?    
    val isReleaseVersion = !project.version.toString().endsWith("SNAPSHOT")

    // use different configurations for snapshots and releases
    cabe {
        if (isReleaseVersion) {
            config.set(Config.StandardConfig.STANDARD.config)
        } else {
            config.set(Config.StandardConfig.DEVELOPMENT.config)
        }
    }
```

**Custom Configuration**

If more control is needed, a custom configuration can be used like in this example:

```
    plugins {
      id("com.dua3.cabe") version "2.1-rc5"
    }
    
    cabe {
        // 
        config.set(Config(Check.NO_CHECK, Check.ASSERT))
    }
```

The first parameter is for public API methods, the second one for private API.

The possible values are:

- NO_CHECK: do not generate any checks
- ASSERT: use standard assertions that can be controlled by JVM parameters
- THROW_NPE: throw NullPointerException for failed null checks
- ASSERT_ALWAYS: throw AssertionError for failed null checks regardless of the JVM assertion settings

Code
----

## cabe-processor

This is the processor that injects assertions into the bytecode. It can be run separately from the command line if
you download the `cabe-processor-all<version>.jar` and run with `java -jar cabe-processor-all<version>.jar <options>`.
Use the option "-h" to display the possible options and their values.

## cabe-gradle-plugin

This module contains the Gradle plugin that applies the processor to the class files.

## cabe-gradle-plugin-test

This module contains tests for the Gradle plugin.

## How to build

Run the shell script `build.sh` to build both packages and run tests. When everything succeeds, the script will
ask if you want to publish the processor library and plugin. Answer `n` unless you have updated the publishing
coordinates and want to publish the plugin.

How does it work?
-----------------

**The plugin** changes the compileJava output folder to `classes-cabe-input`. It then calls `ClassPatcher` to instrument
the compiled classes and write the modified classes to the `classes` folder.

**The **processor** is implemented in the class `ClassPatcher`. It uses the Javassist library to analyse the bytecode
and inject null-checks for method parameters.

Changes
-------

## Version 3.0 (in progress)

- Switch to [JSpecify](https://jspecify.dev) annotations.

## Version 2.1.1

- Do not write ClassPatcher log output to console unless debugging is enabled

## Version 2.1

- Use the standard check for assertion status i.e., query the hidden field $assertionsDisabled instead of calling
  Class.desiredAssertionStatus(). In case the field was not generated by the java compiler, inject the field into
  the class. This leads to a huge speedup because Hotspot is now able to optimize the exception code out when
  exceptions are disabled.
- Error messages for failed checks have been shortened from "parameter 'X' must not be null" to "X is null" to reduce
  the footprint added by the null checks.
- Make the process configurable. See "Plugin configuration" above.
- The processing now runs in a separate thread so that classes loaded during instrumentation cannot
  influence later builds.

## Version 2.0.1

- Revert combining of assertions because the JVM does not seem to recognize the changed assertion pattern which leads to
  a noticeable slowdown when running with assertions disabled.
- Make sure instrumented classes are released when an exception occurs during compilation.

## Version 2.0

To solve the issues described above, I decided to do a full rewrite. I switched to working on the byte code instead
using Javassist instead of SPOON. This seemed easy at first, but it turned out to be a little bit more complex
than I thought. First of all, I had to change the annotation classes because in Version 1.x annotations had SOURCE
retention and were not present in the byte code. That's why you now need version 2.0 of cabe-annotations to work
with the plugin. Also, it proved to be much more complicated to get the mapping of parameters to the actual parameter
names correct. I added lots of test cases to make sure everything works correctly now.

So these are the main changes:

- Annotations now have CLASS retention.
- In derived classes, assertions are checked before super is called.
- Annotations on record parameters work.
- Full support for Java 21.
- The debugging issues are solved.

## Version 1.x

These versions worked on the source level, injecting code into the sources prior to compilation. This worked somewhat
but had the following issues:

- Support for features introduced in newer Java versions lagged behind because all new features had to be supported by
  the parser (SPOON) first.
- In constructors, the assertions could not be inserted before the call to super().
- It was not possible to add assertions for record fields.
- When debugging, sometimes line numbers were off and the debugger showed a message that source code and class file
  didn't match.

## Outlook

Next on my list are these features (in no specific order) that might or might not make it into a future release:

- Add support for annotations on return values.
- Support different annotations like JetBrains or JSpecify. The reason I currently use my own annotations library is
  that I wanted a package wide annotation, @NonNullAPI. While, for example, JSpecify has something comparable, it also
  has annotations on modules and return values, and that is not yet supported.
- Add support for annotation modules (`module-info.java`).
