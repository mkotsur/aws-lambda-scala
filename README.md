[![Codacy Badge](https://api.codacy.com/project/badge/Grade/0fb7e6e25c1846e3b54f836bbb65a24b)](https://www.codacy.com/app/miccots/aws-lambda-scala?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=mkotsur/aws-lambda-scala&amp;utm_campaign=Badge_Grade)
[![Build Status](https://circleci.com/gh/mkotsur/aws-lambda-scala.svg?&style=shield&circle-token=22c35ff0e9c28f61d483d178f8932c928e47dfc2)](https://circleci.com/gh/mkotsur/aws-lambda-scala)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mkotsur/aws-lambda-scala_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.github.mkotsur%22)

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

* JSON (de)serialization of case classes;
* Plain strings are supported too;
* [AWS API Gateway proxy integration](http://docs.aws.amazon.com/apigateway/latest/developerguide/integrating-api-with-aws-services-lambda.html);
* Uncaught errors are logged with SLF4J and re-thrown.

More docs are coming soon... Feel free to look at `src/test/scala` if you want to use it right now.

## Adding to your project

```sbt
libraryDependencies += "io.github.mkotsur" %% "aws-lambda-scala" % {latest-version}
```
## How does aws-lambda-scala compare with Serverless framework
Short answer: they complement each other. Long anwser: read [this blog post](https://medium.com/@mkotsur/this-is-why-you-should-consider-using-aws-lambda-scala-6b3ea841f8b0).
