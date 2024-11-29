# Cabe

Cabe is a Java byte code instrumentation tool that inserts checks based on JSpecify annotations into your class files.

According to the [Fail-Fast Principle](https://www.martinfowler.com/ieeeSoftware/failFast.pdf), all invalid input should be detected and reported early. Cabe helps you
doing this by automatically checking method and constructor parameters.

Cabe also helps you during develompment by checking return values of methods.

<tldr>
<strong>TLDR</strong>

Add this to your Gradle build file to add automatic null-checks to the public API of your project and assertion based
null checks to the private API:

<tabs>
    <tab title="Kotlin DSL">
        <code-block lang="Kotlin">
            plugins {
              id("com.dua3.cabe") version "%PLUGIN_VERSION%"
            }
        </code-block>
    </tab>
</tabs>

Read on for examples and more detailed configuration options.
</tldr>

## What are JSpecify Annotations?

JSpecify is a project that aims to enhance Java code by providing a set of annotations specifically designed to improve 
code quality and facilitate better type-checking. These annotations help developers specify nullability and (later)
other type-related constraints more precisely, allowing for more robust and error-free code. This ultimately leads to 
improved documentation, better IDE support, and more reliable results during static analysis. 

You can find more details about JSpecify on their official [website](https://jspecify.dev).

## What does Cabe do?

Cabe analyzes your classes byte code and injects code that checks for violations of the nullability rules you added
to your code by using JSpecify annotations.

<tip>
The example projects here are contained within the "examples" subproject. To compile and run the examples, use the 
commmand <code>./gradlew -Dexamples examples:&lt;name&gt;:run</code> from the project's main folder.
</tip>
<warning>
Note that running the examples will usually result in a failure as the axamples demonstrate how using Cabe asserts
your programs fail-fast when nullability violations are detected.
</warning>

Let's look at an example (use <code>./gradlew :examples:hello:run</code> to execute):

<code-block lang="Java">
    import org.jspecify.annotations.NonNull;
    
    public class hello.Hello {
        public static void main(String[] args) {
            sayHello(null);
        }

        public static void sayHello(@NonNull String name) {
            System.out.println("hello.Hello, " + name + "!");
        }
    }
</code-block>

The contract of `sayHello(@NonNull String name)` is that `name` must have a non-null value when called. When you run 
this code through Cabe, using the standard configuration, an automatic null-check will be inserted, and when you run
the program, you will see this:

```text
    Exception in thread "main" java.lang.NullPointerException: name is null
    at hello.Hello.sayHello(hello.Hello.java)
    at hello.Hello.main(hello.Hello.java:5)
```

You can of course obtain the same result by using this code:

<code-block lang="Java">
    public void printGreeting(@NonNull String name) {
        Objects.requireNonNull(name, "name is null");
        System.out.println("hello.Hello, " + name);
    }
</code-block>

Have a look at <code>hellofx</code> for another example. In this example, the program is trying to read a resource from
the classpath that can not be found. When not using Cabe, the stack trace will look like this:

```plain text
Exception in Application start method
java.lang.reflect.InvocationTargetException
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:118)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at javafx.graphics/com.sun.javafx.application.LauncherImpl.launchApplicationWithArgs(LauncherImpl.java:464)
	at javafx.graphics/com.sun.javafx.application.LauncherImpl.launchApplication(LauncherImpl.java:364)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/sun.launcher.LauncherHelper$FXHelper.main(LauncherHelper.java:1149)
Caused by: java.lang.RuntimeException: Exception in Application start method
	at javafx.graphics/com.sun.javafx.application.LauncherImpl.launchApplication1(LauncherImpl.java:893)
	at javafx.graphics/com.sun.javafx.application.LauncherImpl.lambda$launchApplication$2(LauncherImpl.java:196)
	at java.base/java.lang.Thread.run(Thread.java:1583)
Caused by: java.lang.NullPointerException: Input stream must not be null
	at javafx.graphics/javafx.scene.image.Image.validateInputStream(Image.java:1140)
	at javafx.graphics/javafx.scene.image.Image.<init>(Image.java:707)
	at hellofx.HelloFX.loadImage(HelloFX.java:21)
	at hellofx.HelloFX.start(HelloFX.java:28)
	at javafx.graphics/com.sun.javafx.application.LauncherImpl.lambda$launchApplication1$9(LauncherImpl.java:839)
	at javafx.graphics/com.sun.javafx.application.PlatformImpl.lambda$runAndWait$12(PlatformImpl.java:483)
	at javafx.graphics/com.sun.javafx.application.PlatformImpl.lambda$runLater$10(PlatformImpl.java:456)
	at java.base/java.security.AccessController.doPrivileged(AccessController.java:400)
	at javafx.graphics/com.sun.javafx.application.PlatformImpl.lambda$runLater$11(PlatformImpl.java:455)
	at javafx.graphics/com.sun.glass.ui.InvokeLaterDispatcher$Future.run(InvokeLaterDispatcher.java:95)
Exception running application hellofx.HelloFX
```

The stacktrace indicates an error happened inside a JavaFX method, two methods down in the stack, our method
<code>loadImage()</code> appears. When we now open our IDE and look at the actual code, it becomes apparent that the 
real problem lies in <code>getResourceAsStream()</code> returning <code>null</code> and not a valid streeam.

Now what happens if Cabe is used? We will get this stacktrace:

```plain text
Exception in Application start method
java.lang.reflect.InvocationTargetException
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:118)
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at javafx.graphics/com.sun.javafx.application.LauncherImpl.launchApplicationWithArgs(LauncherImpl.java:464)
        at javafx.graphics/com.sun.javafx.application.LauncherImpl.launchApplication(LauncherImpl.java:364)
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at java.base/sun.launcher.LauncherHelper$FXHelper.main(LauncherHelper.java:1149)
Caused by: java.lang.RuntimeException: Exception in Application start method
        at javafx.graphics/com.sun.javafx.application.LauncherImpl.launchApplication1(LauncherImpl.java:893)
        at javafx.graphics/com.sun.javafx.application.LauncherImpl.lambda$launchApplication$2(LauncherImpl.java:196)
        at java.base/java.lang.Thread.run(Thread.java:1583)
Caused by: java.lang.AssertionError: invalid null return value
        at hellofx.HelloFX.getResourceAsStream(HelloFX.java:19)
        at hellofx.HelloFX.loadImage(HelloFX.java:23)
        at hellofx.HelloFX.start(HelloFX.java:31)
        at javafx.graphics/com.sun.javafx.application.LauncherImpl.lambda$launchApplication1$9(LauncherImpl.java:839)
        at javafx.graphics/com.sun.javafx.application.PlatformImpl.lambda$runAndWait$12(PlatformImpl.java:483)
        at javafx.graphics/com.sun.javafx.application.PlatformImpl.lambda$runLater$10(PlatformImpl.java:456)
        at java.base/java.security.AccessController.doPrivileged(AccessController.java:400)
        at javafx.graphics/com.sun.javafx.application.PlatformImpl.lambda$runLater$11(PlatformImpl.java:455)
        at javafx.graphics/com.sun.glass.ui.InvokeLaterDispatcher$Future.run(InvokeLaterDispatcher.java:95)
```

Note that the cause now points directly at the exact method that returned the invalid <code>null</code> value. This is
of course a rather trivial example, but it shows how Cabe can simplify your workflow.

<tip>
If you look at the HelloFX source code, you might notice that the method return value was not even marked as
<code>@NonNull</code>. Instead the class was marked as <code>@NullMarked</code>. This basically means that unless
something is explicitly marked as <code>@Nullable</code>, everything inside that class is implicitly treated
as if it were annotated as <code>@NonNull</code>.
</tip>

<note>
The <code>@NullMarked</code> annotation can be applied to classes, packages, and even Jigsaw modules. Read more about
how to use JSpecify annotations in the <a href="https://jspecify.dev/docs/user-guide/">Nullness User Guide</a>.
</note>

## Will using Cabe impact performance?

Depending on the configuration used, there may be a minor overhead. Depending on your requirements, you can use
different configurations.

- During development, it is recommended to enable all checks so that programming errors will show up during early
  testing.

- Once you are certain your project is thoroughly tested, you can restrict parameter checking to methods called by
  third-party code. When the standard setting is used, parameters to private API methods are checked using standard
  assertions that can be enabled or disabled at runtime. Standard assertions are usually optimized out by the JIT
  compiler when assertions are disabled. There may still be a minor impact due to increased class file size.

- You can also disable all checks by using the <code>NO_CHECK</code> configuration and there will be no performance
  hit at all. Be aware that you might trade correctness to speed in this case as invalid inputs will not be detected.

When in doubt, profile your application when compiled using the different settings.

## How are Null Checks implemented?

This depends on the configuration used. There are four types of checks. The examples assume a non-nullable parameter
#named `p`.

| Check              | Equivalent Java code                                            |
|--------------------|-----------------------------------------------------------------|
| NO_CHECK           | [N/A]                                                           |
| STANDARD_ASSERTION | `assert p!=null : "p is null";`                                 |
| ASSERT_ALWAYS      | `if (p==null) throw new AssertionError("p is null");`           |
| THROW_NPE          | `if (p==null) throw new NullPointerException("p is null");`     |
| THROW_IAE          | `if (p==null) throw new IllegalArgumentException("p is null");` |

## Public vs Private API

When developing a library, you can configure different checks for:

- the **public API** of your library so that invalid parameter values are detected when the user of your library calls your
  code with disallowed <code>null</code> values for a parameter,

- the **private API** of your library, i.e., code that cannot be directly called by users of your library.

<note>
In the standard configuration, parameters that are part of the public API will always be checked and throw a
<code>NnullPointerException</code> when an <code>null</code> parameter value is detected where it is not allowed
and for parts of your private API, standard assertions are used that can be enabled at runtime using the standard
<code>-ea</code> flag.
</note>

## What are the predefined configurations?

The predefined configurations are:

| Name        | Public API    | Private API   | Return Values |
|-------------|---------------|---------------|---------------|
| DEVELOPMENT | ASSERT_ALWAYS | ASSERT_ALWAYS | ASSERT_ALWAYS |
| STANDARD    | THROW_NPE     | ASSERT        | NO_CHECK      |
| NO_CHECK    | NO_CHECK      | NO_CHECK      | NO_CHECK      |

## Things to note

Here are some points that you should be aware of when using Cabe.

### Records

Cabe supports Java Records.

#### Do I need to explicitly add a Record Constructor so that Parameters can be checked?

No, Cabe adds the checks by evaluating the record declaration:

<code-block lang="Java">
    record MyRecord(@NonNull String a, @Nullable String b) {}

    void foo() {
        // this works
        MyRecord r1 = new MyRecord("a", null);
        // this will throw an exception "a is null"
        MyRecord r1 = new MyRecord(null, "b");
    }
</code-block>

#### Standard Assertions cannot be generated for Record classes

Cabe currently cannot inject standard assertions into record classes because of technical restrictions. That is why for
records, THROW_NPE is used instead of ASSERT. ASSERT_ALWAYS works as it does for other classes.

**Technical background** 

Standard assertions use a special boolean flag <code>$assertionsDisabled</code>
that is initialised by the JVM when the class is loaded to the value obtained by calling
<code>Class.getDesiredAssertionStatus()</code>.

For classes that do not contain any assertions
in their source code, this flag is not present in the class file and has to be injected into the byte code.
the initialisation is then done in a static initializer block. This does not work for records and results in an
<code>InvalidClassFileException</code>.

### Arrays

Cabe will detect when <code>null</code> is passed for a non-nullable array parameter. It will however not detect null
values contained in an array.

<code-block lang="Java">
    void foo(@NonNull String[] array) {}
    void bar(@NonNull String @NonNull[] array) {}

    void test() {
        // exception will be thrown
        foo(null);
        // no exception will be thrown although null elements are disallowed
        bar(new String[] {"123", null});
    }
</code-block>

### Generics

Cabe will detect violations for generic parameters when it can be determined at compile time that a type is non
nullable:

<code-block lang="Java">
    @NullMarked
    class Generic&lt;T, U extends @Nullable String&gt; {

        void foo(T t, U u) {}

        void test() {
            // will throw a NullPointerException
            foo(null, "u");

            // will not throw
            foo("t", null);
        }
    }
</code-block>

If cannot check parameters where the nullability can not be determined at compile time:

<code-block lang="Java">
    @NullUnmarked
    class Generic2&lt;T&gt; {
        void foo(T t) {}
    }

    {
        // this does not throw, null is allowed
        new Generic2&lt;String&gt;().foo(null);

        // this also does not throw because the nullability can not be determined at compile time of foo() 
        new Generic2&lt;@NonNull String&gt;().foo(null);
    }
</code-block>

### SpotBugs

If you use SpotBugs in your build, it may report unnecessary null checks, i.e., when Cabe is configured to check method
return values and SpotBugs byte code analysis infers the checked value will be non-null anyway. In that case, you might
want to use a SpotBugs exclusion file.

<code-block lang="XML">
    &lt;FindBugsFilter&gt;
        &lt;Match&gt;
            &lt;!-- Bugs reported by SpotBugs for automatic injected null checks --&gt;
            &lt;Or&gt;
                &lt;Bug pattern="RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE"/&gt;
                &lt;Bug pattern="RCN_REDUNDANT_COMPARISON_OF_NULL_AND_NONNULL_VALUE"/&gt;
                &lt;Bug pattern="SA_LOCAL_SELF_ASSIGNMENT" /&gt;
            &lt;/Or&gt;
        &lt;/Match&gt;
    &lt;/FindBugsFilter&gt;
</code-block>

### Using Cabe in your Gradle Build

Cabe can be used either as a standalone program that you can run manually to instrument your class files or as a Gradle
plugin that runs automatically in your build process. Let's see how it is done with Gradle.

To use Cabe in your Gradle build, add the plugin to your build script and configure the plugin:

<tabs>
    <tab title="Kotlin DSL">
        <code-block lang="Kotlin">
            plugins {
              id("com.dua3.cabe") version "%PLUGIN_VERSION%"
            }
        </code-block>
    </tab>
    <tab title="Groovy DSL">
        <code-block lang="groovy">
            plugins {
              id "com.dua3.cabe" version "%PLUGIN_VERSION%"
            }        
        </code-block>
    </tab>
</tabs>

This will run the Cabe processor in your build. When no configuration is given, a standard configuration is used.

#### Configure the Cabe Task

To configure the instrumentation, you can configure Cabe like this to use one of the predefined configurations:

<tabs>
    <tab title="Kotlin DSL">
        <code-block lang="Kotlin">
            cabe {
                config.set(Configuration.STANDARD)
            }
        </code-block>
    </tab>
    <!-- FIXME add Groovy syntax -->
</tabs>

If you omit the configuration block in your build, the standard configuration will be used.

#### Using different Configurations for Development and Release Builds

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

### Using as a Cabe as a standalone Command Line Tool

The instrumentation is done by the `ClassPatcher` class. A precompiled runnable Jar that includes all necessary dependencies can be downloaded from
[Maven Central Repository](https://mvnrepository.com/artifact/com.dua3.cabe/cabe-processor-all/%PROCESSOR_VERSION%) and run using `java -jar`:

```text
    % curl https://repo1.maven.org/maven2/com/dua3/cabe/cabe-processor-all/3.0-rc/cabe-processor-all-%PROCESSOR_VERSION%-rc.jar -o cabe-processor-all.jar
      % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                     Dload  Upload   Total   Spent    Left  Speed
    100 2955k  100 2955k    0     0   973k      0  0:00:03  0:00:03 --:--:--  973k
    
    % java -jar cabe-processor-all.jar --help
    ClassPatcher
    ============
    
    Add null checks in Java class file byte code.
    
    Usage: java -jar <jar-file> -i <input-folder> -o <output-folder> [-c <configuration>] [-cp <classpath>] [-v <verbosity>]
    
        <configuration>  : STANDARD|DEVELOPMENT|NO_CHECKS|<configstr> (default: STANDARD)
    
                           STANDARD    - use standard assertions for private API methods,
                                         throw NullPointerException for public API methods
                           DEVELOPMENT - failed checks will always throw an AssertionError, also checks return values
                           NO_CHECKS   - do not add any null checks (class files are copied unchanged)
                           <configstr> - configuration string as described in the documentation
    
        <verbosity>      : 0 - show warnings and errors only (default)
                         : 1 - show basic processing information
                         : 2 - show detailed information
                         : 1 - show all information
```

## What Java version is Cabe compatible with?

Cabe needs at least Java 17 to run. The instrumentation should work for class files from Java 11, but I have only
tested this with versions 17 to 23.

## Is there a Maven Plugin for Cabe?

Not yet. But it's on my Todo list.

## Is the Source available?

Sourcecode is available under the [MIT license](https://github.com/xzel23/cabe/blob/main/LICENSE) on the project 
[GitHub page](https://github.com/xzel23/cabe).

## Where do I report Bugs?

Use [GitHub issues](https://github.com/xzel23/cabe/issues) to report Bugs and suggestions.

## What about the Name?

In Javanese, both cabe and lombok refer to chili peppers. At the same time, Lombok is a... well, it's something between
a library and a language on its own that extends Java with certain features. One of these features are annotations to
mark nullable and non-nullable types and code instrumentation to do runtime checks based on these annotations.

While widely used, Lombok is quite controversial - you will find plenty of discussions on this topic on the internet.

Newer Java versions brought many features that developers used Lombok for, perhaps most notably Java records. But
having automated null checks in your code is one Lombok feature that I liked but could not find any non-Lombok
alternative. That's why I started Cabe, and that's where the name comes from.
