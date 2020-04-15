# Frequently asked questions

### Why am I seeing `ExecutionException`?

When a Future's computation fails due to an `java.lang.Error`, the `Future` stores a `java.util.concurrent.ExecutionException` with the actual `Error` as its cause.

This library creates a new `java.lang.Error` only in one place, which is outside of `Future`. So, if you are annoyed by this extra layer of stacktrace -- ensure your code doesn't return `java.lang.Error` as a failure of the Future-based lambda function.

See also:
* [Scala Docs: Exceptions](https://docs.scala-lang.org/overviews/core/futures.html#exceptions)
* [Scala Puzzlers: An Exceptional Future
](https://scalapuzzlers.com/#pzzlr-056)

