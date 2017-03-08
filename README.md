[![Build Status][ci-img]][ci]
[![Released Version][maven-img]][maven]

# Java reflection utilities

This library contains utility classes related to reflection.  
I found myself re-implementing these type of methods over and over again 
whenever I needed reflection in some project.
As there seldom is enough time to do this properly and take things like caching
into consideration, the solutions were always sub-optimal.
That is why this library was created.

## Utility-class `Classes`

For finding classes and interacting with them.

## Utility-class `Methods`

For finding methods and invoking them.

## Utility-class `Constructors`

For finding constructors and invoking them.

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
contains named field appenders. It can easily be instantiated by the static `reflect` method.

## Package 'errorhandling'

Contains the exception types that can be thrown by this library,
all subclasses of `java.lang.RuntimeException`.


  [ci-img]: https://img.shields.io/travis/talsma-ict/reflection/master.svg
  [ci]: https://travis-ci.org/talsma-ict/reflection
  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware/reflection.svg
  [maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22nl.talsmasoftware%22%20AND%20a%3A%22reflection%22
