version 3.2.0
=============

- make the cabe task cacheable
- dependency updates
- should be commpatible with upcoming Gradle 9 (tested with RC 3)
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
