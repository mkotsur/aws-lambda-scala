[![Codacy Badge](https://api.codacy.com/project/badge/Grade/0fb7e6e25c1846e3b54f836bbb65a24b)](https://www.codacy.com/app/miccots/aws-lambda-scala?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=mkotsur/aws-lambda-scala&amp;utm_campaign=Badge_Grade)
[![Known Vulnerabilities](https://snyk.io/test/github/mkotsur/aws-lambda-scala/badge.svg?targetFile=build.sbt)](https://snyk.io/test/github/mkotsur/aws-lambda-scala?targetFile=build.sbt)
[![Build Status](https://circleci.com/gh/mkotsur/aws-lambda-scala.svg?&style=shield&circle-token=22c35ff0e9c28f61d483d178f8932c928e47dfc2)](https://circleci.com/gh/mkotsur/aws-lambda-scala)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mkotsur/aws-lambda-scala_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.github.mkotsur%22)
[![Join the chat at https://gitter.im/software-farm/aws-lambda-scala](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/software-farm/aws-lambda-scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


## Breaking changes

* Method `handle` is now required to take a context! It's still up to you if you use it or not, though...
* `handleRequest` is now the entry point into the lambda
* FLambda: bring your own effect
* If an error is thrown in the handler - it will be rethrown.
* If the effect handler returns a "failure" - it will be logged and rethrown at the "end of the world".
* No logging
* You can't return `Either[Future[_]]` anymore. Pick one of them!

Writing a handler for AWS lambda in Scala can be as easy as...

```scala
import io.circe.generic.auto._
import io.github.mkotsur.aws.handler.Lambda._
import io.github.mkotsur.aws.handler.Lambda
import com.amazonaws.services.lambda.runtime.Context

case class Ping(inputMsg: String)

case class Pong(outputMsg: String)

class PingPongHandler extends Lambda[Ping, Pong] {

  override def handle(ping: Ping, context: Context) = Right(Pong(ping.inputMsg.reverse))

}
```
The input JSON will be automatically de-serialized into `Ping`, and the output into `Pong`. The `handle()` method is supposed to return `Either[Throwable, Pong]`: `Right` if the input was handled correctly, and `Left` otherwise. 

This handler can be used in AWS Lambda as: `io.github.mkotsur.example::handle`.

Features:

* Return Futures right from the handler!
* JSON (de)serialization of case classes;
* Plain strings are supported too;
* [AWS API Gateway proxy integration](http://docs.aws.amazon.com/apigateway/latest/developerguide/integrating-api-with-aws-services-lambda.html);
* Uncaught errors are logged with SLF4J and re-thrown.

More docs are coming soon... Feel free to look at `src/test/scala` if you want to use it right now.

## Examples

### Returning futures

```scala
import io.circe.generic.auto._
import io.github.mkotsur.aws.handler.Lambda._
import io.github.mkotsur.aws.handler.Lambda
import com.amazonaws.services.lambda.runtime.Context
import scala.concurrent.Future

case class Ping(inputMsg: String)

class PingFuturePongHandler extends Lambda[Ping, Future[Int]] {

  override def handle(ping: Ping, context: Context) = 
    Right(Future.successful(ping.inputMsg.length))

}
```

### Not receiving and not returning any value

This lambda will accept an empty string, or string with `null` as an input.

```scala
import io.circe.generic.auto._
import io.github.mkotsur.aws.handler.Lambda._
import io.github.mkotsur.aws.handler.Lambda
import com.amazonaws.services.lambda.runtime.Context

class NothingToNothingHandler extends Lambda[None.type, None.type] {

  override def handle(_: None.type , context: Context) = {
    println("Only side effects") 
    Right(None)
  }
    
}
```


## Adding to your project

Scala versions supported: 2.11.x, 2.12.x, 2.13.x.

```sbt
libraryDependencies += "io.github.mkotsur" %% "aws-lambda-scala" % {latest-version}
```
## How does aws-lambda-scala compare with Serverless framework
Short answer: they complement each other. Long answer: read [this blog post](https://medium.com/@mkotsur/this-is-why-you-should-consider-using-aws-lambda-scala-6b3ea841f8b0).
