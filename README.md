Cabe
====

From the [disambiguation page for Lombok](https://id.wikipedia.org/wiki/Lombok_(disambiguasi)) in the indonesian Wiklipedia:

_Lombok adalah nama lain dari cabai_ (Lombok is another name for cabai).

This started out when I annotated one of my library projects with Jetrains @NotNull annotations and found out, that while very helpful, this doesn't help me at all in projects that use the library. I know that project Lombok provides similar annotations that also guarantee, but I did not want to add runtime dependencies to my project.

When using @NotNull in IntelliJ, a runtime assertion is injected into the resulting bytecode, and I think that's a cool thing to have:

 - you can run your code with assertions enabled
 - when you don't enable assertions, you have virtually no overhead
 - static analysis tools can inspect the bytecode and infer which parameters are asserted not be null and output warnings where this contract is not fulfilled  

I started looking around to see how I could enable this in my gradle build, but did not find any solution. I also did not want to use Lombok because the Lombok assertions have runtime rentention and add a runtime dependency to projects that use it. 

Also Lombok tends to introduce new features that while innovative later are implemented in a different way in standard JDK.

What I wanted is something more or less like Lombok, but closer to standard Java. Thus the name was born: Cabai is Lombok, but on the island of Java itself, it's commonly called Cabe!

Usage
-----

**NOTE: The plugin is not yet released to the Gradle plugin portal, so at the moment you have to build the plugin locally. Once I am confident the plugin has reached a somewhat stable state, I will release to the plugin portal and remove zhis notice.**



