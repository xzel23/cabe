version 4.2.0 ( in delevopment)
===============================

### Breaking Changes

- Configurations now default to 'strict' mode. That means that code that overrides `Object.equals(Object)`
  in a `@NullMarked` context and does not annotate the argument as  `@Nullable` will be rejected.
  This ensures that code does not violate that
  ["for any non-null reference value x, x.equals(null) should return false"]
  (https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Object.html#equals(java.lang.Object)).
  
  This can be disabled by configuring non-strict mode using `wtihStrict(false)`:
  ```kotlin
  cabe {
    // Use com.dua3.cabe.processor.Configuration
    config.set(com.dua3.cabe.processor.Configuration.STANDARD).wtihStrict(false)
  }
  ```

### Fixes

- Fixed a bug where parameter names were incorrect in the presence of long and double parameters. (issue #25)
- Fixed a bug when building the Maven plugin on Windows.
- Updated dependencies JUnit and Log4J.

### Features

- Added compatibility tests to the build:
    - Gradle version compatibility: 8.6, 8.14, 9.0, 9.4, and the version the build was started with.
    - Gradle configuration cache compatibility (Gradle 8.14+) only: 8.14, 9.0, 9.4, and the version
      the build was started with.
    - JDK version compatibility: JDK versions 17, 21, and 25
- The configuration parser now supports directly setting strict mode (e.g., `ASSERT:strict=false`),
  strict mode defaults to `true`.

### Other changes

- Refactored the build from using a shell script to control build and test executions before eventually releasing
  the artifacts.
- Updated the Gradle wrapper and dependencies to current versions.
- Updated the GitHub Actions workflow.
- Refactored the internal handling of strict mode in the extension

version 4.1
============

- Introduced a `strict` mode option to enforce the `equals(Object)` contract. 
  When enabled, instrumentation will fail if `equals(Object)` is overridden with 
  a non-nullable parameter. In non-strict mode (default), a warning is logged 
  and the parameter is automatically treated as nullable.


version 4.0.x
=============

bugfix updates
- fix: #22 was only partly resolved
- fix: configuration cache incompatibilities (#23)
- fix: instrumentation failure on windows (#24)

To ensure less problems in future updates, the build now also executes extra tests with the configuration cache enabled
and tests are run on linux, macOS, and Windows.

version 4
=========

- Added a check that equals overrides are declared correctly, i.e., the argument must be nullable for the equals() 
  contract "x.equals(null) must return false" to be fulfilled. 
- Fixed an issue where instrumentation failed when modules with optional dependencies were on the module path. (#22)
- The hello-maven example now is built when the plugin is now also built using the build.sh script.
- improve compatibility with Gradle build and configuration caches

version 3.3.0
=============

- Gradle 9 compatibility. The explicit `cabe` task has been removed and the
  instrumentation is now run as a doLast() action.
- Support running the plugin with the Gradle configuration cache enabled.
- Migrate from OSS-RH to Maven Publish Portal; use JReleaser for publishing.
- Automatically publish CI builds

version 3.2.0
=============

- make the cabe task cacheable
- dependency updates
- ~~should be commpatible with upcoming Gradle 9 (tested with RC 3)~~
  
  __Version 3.2.0 is unfortunately not fully compatible with Gradle 9!__

  While neither warnings nor deprecations are shown,
  processed classes do not end up in the correct build/classes directory.
  This is not a problem for most tasks (like jar) since task outputs are configured
  and subsequent tasks use the processed files. Third party tools however
  might not find the class files or have to be configured differently.

  Such silently breaking changes make supporting Gradle plugins a frustrating
  experience. Even more so when the internal Gradle code has everything in
  place to do this right, but what is the point of using factories and
  separating interface from implementation when use of the default factories
  and implementations is hard-coded in private methods that cannot be
  overridden?

  Anyone interested in details: in Gradle 9, changing the compileJava destination
  output dir automatically changes the sourceset output dir and there is no way
  around this.
  
  Version 9 compatibility will be in the next release (currently testing).
  
- the cabe processor version did not change (still 3.1.0)

version 3.1.0
=============

- fix annotations on enum constructors not working correctly due to a bug in the Java compiler
  details: [Cabe#18](https://github.com/xzel23/cabe/issues/18)
- fix compilation error on systems that do not have JavaFX set up
- use Gradle toolchains
- fix some Gradle deprecation warnings

version 3.0.2
=============

- fix: duplicated jars in classpath (contributed by mk868)
- Maven plugin (contributed by mk868)

version 3.0.1
=============

- introduce Check.THROW_IAE to throw IllegalArgumentException instead of NullPointerException

version 3.0.0
=============

- initial release with support for jspecify annotations
