version 3.3.0-rc
================

- Gradle 9 compatibility. The explicit `cabe` task has been removed and the
  instrumentation is now run as a doLast() action.
- Support running the plugin with the Gradle configuration cache enabled.

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
