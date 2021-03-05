[![CI build][ci-img]][ci]
[![Coverage Status][coveralls-img]][coveralls]
[![Maven Version][maven-img]][maven]
[![JavaDoc][javadoc-img]][javadoc]

# Java reflection utilities

This library contains utility classes related to reflection.  
I found myself re-implementing these type of methods over and over again 
whenever I needed reflection in some project.
As there seldom is enough time to do this properly and take things like caching
into consideration, the solutions were always sub-optimal.
That is why this library was created.

## Contributing

Have you _found a bug_? Did you _create new featue_?  
Please see the [contributing page](CONTRIBUTING.md) for more information.

## Utility-class `Classes`

For _getting_ or _finding_ classes and interacting with them.  
The difference between getting and finding is that a `get` operation
throws exceptions if nothing is found, and `find` returns `null`.

## Utility-class `Methods`

For _getting_ or _finding_ methods and invoking them.

## Utility-class `Constructors`

For _getting_ or _finding_ constructors and invoking them.

## Package 'beans'

Containing the `BeanReflection` class, this package provides access to 
Java's built-in `Introspector.getBeanInfo` but falls back to public field access
when available.

Furthermore, the properties can be 'chained' safely concatenating them 
with dots inbetween (`'.'`). Array indices are also supported by square brackets (`'['` and `']'`).

## Package 'dto'

The `AbstractDto` superclass can be extended when your class is merely a *Data Transfer Object*.
Such DTO's are all about simply representing a datastructure and normally don't need any methods.
Subclasses merely need to provide public fields containing their datastructure
and will be automatically provided with `equals`, `hashCode`, `toString` and `clone`
implementations including all accessible fields from the subclass.

## Package 'strings'

The `ToStringBuilder` is a convenient builder for `toString()` representations 
containing named field appenders. It is part of the reflection library because 
it can easily be instantiated for any bean by the static `reflect` method.

## Package 'errorhandling'

Contains the exception types that can be thrown by this library,
all subclasses of `java.lang.RuntimeException`.

# Java 9 module concerns

If you're using the Java 9 module system (e.g. project _jigsaw_),
you can still use this reflection library. Actually the library itself is
neatly published as a java 9 module called `nl.talsmasoftware.reflection`
even though the classes are still java 5 binary compatible.

However, out of the box, our module cannot reflect your classes if you haven't _opened_ them up.
So be sure to either declare your module as an `open` module or explicitly open up a 
package for reflection by this module.

Prepend your module with the `open` keyword to open it up for reflection like this:
```java
open module com.example.myapp {
    requires nl.talsmasoftware.reflection;
}
```

Or you can define a specific package for reflection:
```java
module com.example.myapp {
    opens com.example.myapp.dto to nl.talsmasoftware.reflection;
    requires nl.talsmasoftware.reflection;
}
```

## License

[Apache 2.0 license](../LICENSE)


  [ci-img]: https://github.com/talsma-ict/reflection/actions/workflows/ci-build.yml/badge.svg
  [ci]: https://github.com/talsma-ict/reflection/actions/workflows/ci-build.yml
  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware/reflection
  [maven]: http://mvnrepository.com/artifact/nl.talsmasoftware/reflection
  [coveralls-img]: https://coveralls.io/repos/github/talsma-ict/reflection/badge.svg
  [coveralls]: https://coveralls.io/github/talsma-ict/reflection
  [javadoc-img]: https://www.javadoc.io/badge/nl.talsmasoftware/reflection.svg
  [javadoc]: https://www.javadoc.io/doc/nl.talsmasoftware/reflection 

