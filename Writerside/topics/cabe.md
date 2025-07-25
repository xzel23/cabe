# Cabe

Cabe is a Java byte code instrumentation tool that inserts checks based on JSpecify annotations into your class files.

According to the [Fail-Fast Principle](https://www.martinfowler.com/ieeeSoftware/failFast.pdf), all invalid input should
be detected and reported early. Cabe helps you
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

Note that the cause now points directly at the exact method that returned the invalid <code>null</code> value:

```
Caused by: java.lang.AssertionError: invalid null return value
        at hellofx.HelloFX.getResourceAsStream(HelloFX.java:19)
```

This is of course a rather trivial example, but it shows how Cabe can simplify your workflow.

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

## What makes Cabe different from other projects like Nullaway?

NullAway does static analysis and reports possible problems in your code. Cabe instead inserts runtime checks.
But what does this mean in practice?

### As a library developer

Even if you use NullAway (or other static analyzers like Sonar, Qodana, etc.), the users of your library might not.
This means you still should check every parameter your library is passed from user code ("fail early").

Let's look at an example. We have a public method that is callable from user code:

```
        @NullMarked
        public static void sayHelloOnDay(String name, String day) {
            System.out.println("Hello, " + name + ", today is " + day +".);
        }
```

NullAway will report when you use it in your own code, but it cannot tell whether the user of your library
will always call it with non-null parameters. To make it fail early, you need to check the parameters:

```
        @NullMarked
        public static void sayHelloOnDay(String name, String day) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(day);
            System.out.println("Hello, " + name + ", today is " + day +".);
        }
```

This works, but when a `NullPointerException` is thrown, there is no information about which of the parameters
caused it. So you better do it like this:

```
        @NullMarked
        public static void sayHelloOnDay(String name, String day) {
            Objects.requireNonNull(name, "name is null");
            Objects.requireNonNull(day), "day is null");
            System.out.println("Hello, " + name + ", today is " + day +".);
        }
```

Much better. But you know have to remember to change the messages too should you ever change parameter names.
And of course, you have the maintenance cost of always making sure your checks match the method declaration.

Using the Cabe plugin, this code:

```
        @NullMarked
        public static void sayHelloOnDay(String name, String day) {
            System.out.println("Hello, " + name + ", today is " + day +".);
        }
```

will be replaced at compilation time by:

```
        @NullMarked
        public static void sayHelloOnDay(String name, String day) {
            if (name == null) {
                throw new NullPointerException("name is null);
            }
            if (day == null) {
                throw new NullPointerException("day is null);
            }
            System.out.println("Hello, " + name + ", today is " + day +".);
        }
```

You don't need to manually write all those null checks for parameters.

### As an application developer

During development, you can configure Cabe to unconditionally throw `AssertionError` on violations. This can
be useful for example when you use a framework that silently swallows `NullPointerException` under certain
conditions, for example when using JavaFX event handlers or `Platform.runLater()`. JavaFX will log those events
but the application continues to run, so this might go undetected during testing.

You can also enable strict checking for private methods parameters and return values during development
and then disable the checks for your private code for release versions, making the development version of
your software exit with an `AssertionError` and doing no checks or throwing `NullPointerException` in the
release version. This can be useful when you have time critical code where you don't want these additional
checks to be done at runtime for the release version.

## Doesn't Lombok offer similar functionality?

Yes, AFAIK Lombok has runtime null check injection. Differences are:

- You have less control over the kind of checks (for example, Cabe allows you to use assertions for private methods
  and throwing NPE for public ones).
- Lombok does not offer runtime checks for method return values.
- Lombok does not offer runtime checks for record components, you'd have to explicitly define a record constructor.
- Using Lombok is controversial since some consider "lomboked" code to not be valid Java anymore but rather using
  a language based on Java: Depending on what features of Lombok you use, your code might not be compilable by
  the standard JDK compiler anymore. (This is a rather heated discussion, and I rather not chime in.)

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

- the **public API** of your library so that invalid parameter values are detected when the user of your library calls
  your
  code with disallowed <code>null</code> values for a parameter,

- the **private API** of your library, i.e., code that cannot be directly called by users of your library.

<note>
In the standard configuration, parameters that are part of the public API will always be checked and throw a
<code>NullPointerException</code> when an <code>null</code> parameter value is detected where it is not allowed
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

### Using with Java Modules

When using Cabe in a modular project, make sure your module declaration (`module-info.java`) requires the JSpecify
annotations:

```java
module com.example.myapp {
    requires org.jspecify;
    // other module declarations
}
```

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

Read [](cabe-gradle-plugin.md) for Gradle instructions.

### Using Cabe in your Maven Build

Read [](cabe-maven-plugin.md) for Maven instructions.

### Using as a Cabe as a standalone Command Line Tool

Read [](cabe-standalone-processor.md) for instructions on how to use the standalone processor.

## What Java version is Cabe compatible with?

Cabe needs at least Java 17 to run. The instrumentation should work for class files from Java 11, but I have only
tested this with versions 17 to 24.

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
