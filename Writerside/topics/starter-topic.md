# How to Use

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
commmand <code>./gradlew :examples:&lt;name&gt;:run</code> from the project's main folder.
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

## How are Null Checks implemented?

This depends on the configuration used. There are four types of checks. The examples assume a non-nullable parameter
#named `p`.

| Check              | Java code                                                   |
|--------------------|-------------------------------------------------------------|
| NO_CHECK           | [N/A]                                                       |
| STANDARD_ASSERTION | `assert p!=null : "p is null";`                             |
| ASSERT_ALWAYS      | `if (p==null) throw new AssertionError("p is null");`       |
| THROW_NPE          | `if (p==null) throw new NullPointerException("p is null");` |

## Why should I use Instrumentation on my Code?

According to the [Fail-Fast Principle](https://www.martinfowler.com/ieeeSoftware/failFast.pdf), all invalid input should
be detected and reported early.

Let's look at an example:

<code-block lang="Java">
    import javafx.fxml.FXMLLoader;
    import javafx.scene.Parent;
    import javafx.scene.Scene;
    import javafx.stage.Stage;
    import javafx.application.Application;
    import java.net.URL;
    
    public class FXMLController extends Application {
    
        private final URL fxmlUrl;
    
        public FXMLController(URL fxmlUrl) {
            this.fxmlUrl = fxmlUrl;
        }
    
        @Override
        public void start(Stage primaryStage) throws Exception {
            Parent root = FXMLLoader.load(fxmlUrl);
            primaryStage.setTitle("FXML Example");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        }
    
        public static void main(String[] args) {
            URL url = FXMLController.class.getResource("/application.fxml");
            FXMLController controller = new FXMLController(url);
            Application.launch(controller.getClass(), args);
        }
    }
</code-block>

This is a classic. It will probably run perfectly well when started from within your IDE, but once you try running your
project from a JAR file, you might get a `NullPointerException` in `FXMLLoader`. When you try to reproduce the exception
from within your IDE, everything works again. If you follow any JavaFX forum, you probably will have seen many posts
by beginners in JavaFX development about "NullPointerException in FXMLLoader.load()".

Now let's use JSpecify annotations

<code-block lang="Java">
    ...
    import org.jspecify.annotations.NonNull;

    public class FXMLController extends Application { 
        ...

        public FXMLController(@NonNull URL fxmlUrl) {
            this.fxmlUrl = fxmlUrl;
        }

        ...
    }
</code-block>

We assume you have included the Cabe plugin in your build. Now, when you run your program, you will see this:


## Use Cabe in your Gradle Build

Cabe can be used either as a standalone program that you can run manually to instrument your class files or as a Gradle
plugin that runs automatically in your build process. Let's see how it is done with Gradle.

To use Cabe in your Gradle build, add the plugin to your build script and configure the plugin:

<tabs>
    <tab title="Kotlin DSL">
        <code-block lang="Kotlin">
            plugins {
              id("com.dua3.cabe") version "3.0-beta-10"
            }
        </code-block>
    </tab>
    <tab title="Groovy DSL">
        <code-block lang="groovy">
            plugins {
              id "com.dua3.cabe" version "3.0-beta-10"
            }        
        </code-block>
    </tab>
</tabs>

This will run the Cabe processor in your build. When no configuration is given, a standard configuration is used.

### Configure the Cabe Task

To configure the instrumentation, you can configure Cabe like this to use one of the predefined configurations:

<tabs>
    <tab title="Kotlin DSL">
        <code-block lang="Kotlin">
            cabe {
                config.set(Configuration.StandardConfig.STANDARD.config())
            }
        </code-block>
    </tab>
    <!-- FIXME add Groovy syntax -->
</tabs>

If you omit the configuration block in your build, the standard configuration will be used.

### Using different Configurations for Development and Release Builds

You can also automatically select a configuration based on your version string. In this example, strict checking is done
for snapshot and beta versions whereas a release build will use the standard configuration:

<tabs>
    <tab title="Kotlin DSL">
        <code-block lang="Kotlin">
            val isSnapshot = project.version.toString().toDefaultLowerCase().contains("snapshot")
            cabe {
                if (isSnapshot) {
                    config.set(Configuration.StandardConfig.DEVELOPMENT.config())
                } else {
                    config.set(Configuration.StandardConfig.STANDARD.config())
                }
            }
        </code-block>
    </tab>
    <!-- FIXME add Groovy syntax -->
</tabs>
