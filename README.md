[![Codacy Badge](https://api.codacy.com/project/badge/Grade/0fb7e6e25c1846e3b54f836bbb65a24b)](https://www.codacy.com/app/miccots/aws-lambda-scala?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=mkotsur/aws-lambda-scala&amp;utm_campaign=Badge_Grade)
[![Known Vulnerabilities](https://snyk.io/test/github/mkotsur/aws-lambda-scala/badge.svg?targetFile=build.sbt)](https://snyk.io/test/github/mkotsur/aws-lambda-scala?targetFile=build.sbt)
[![Build Status](https://circleci.com/gh/mkotsur/aws-lambda-scala.svg?&style=shield&circle-token=22c35ff0e9c28f61d483d178f8932c928e47dfc2)](https://circleci.com/gh/mkotsur/aws-lambda-scala)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mkotsur/aws-lambda-scala_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.github.mkotsur%22)
[![Join the chat at https://gitter.im/software-farm/aws-lambda-scala](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/software-farm/aws-lambda-scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

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
  
  override protected def handle(i: None.type, c: Context) = {
    println("Only side effects")
    Right(None)
  }

}
```

### API Gateway proxy integration

You can write less boilerplate when implementing a handler for API Gateway proxy events by extending `Lambda.ApiProxy[I, C, O]`. There are three type parameters there. The first one (`I`) corresponds to the `body` field of the `API Gateway proxy event`, the second one (`C`) corresponds to `requestContext` field, and the third one – to the `body` in the response object. More info about how the even looks like [here](https://docs.aws.amazon.com/lambda/latest/dg/services-apigateway.html).

```scala
import io.circe.generic.auto._
import io.circe.Json
import io.github.mkotsur.aws.handler.Lambda._
import io.github.mkotsur.aws.proxy._
import io.github.mkotsur.aws.handler.Lambda
import com.amazonaws.services.lambda.runtime.Context
import MyProxy._

object MyProxy {

  case class MyRequestBody(name: String)

  case class MyResponseBody(score: Int)

}

class MyProxy extends Lambda.ApiProxy[MyRequestBody, Json, MyResponseBody] {
  override def handle(
                                 i: ApiProxyRequest[MyRequestBody, Json],
                                 c: Context
                               ): Either[Throwable, ApiProxyResponse[MyResponseBody]] =
    i.body match {
      case Some(MyRequestBody("Bob")) =>
        Right(ApiProxyResponse.success(Some(MyResponseBody(100))))
      case Some(MyRequestBody("Alice")) =>
        Right(ApiProxyResponse.success(Some(MyResponseBody(50))))
      case Some(_) =>
        Right(ApiProxyResponse(404))
      case None =>
        Left(new IllegalArgumentException)
    }
}

```

Tip 1: of course, you can also pass a type defined by a case class into the second type parameter. Please check #24 and `src/test/scala/io/github/mkotsur/aws/proxy/ProxyRequestTest.scala` for an example.

Tip 2: Don't forget that `Lambda.ApiProxy` is a very thin wrapper around `Lambda`, so if something in `ApiProxyRequest` or `ApiProxyResponse` doesn't work for you - feel free to define your own case classes (and if you believe that the use case is generic enough – consider contributing back to the library).

### Other usages

Feel free to look at `src/test/scala` for more examples. And of course, contributions to the docs are welcome!


## Adding to your project

Scala versions supported: 2.11.x, 2.12.x, 2.13.x.

```sbt
libraryDependencies += "io.github.mkotsur" %% "aws-lambda-scala" % {latest-version}
```
## How does aws-lambda-scala compare with Serverless framework
Short answer: they complement each other. Long answer: read [this blog post](https://medium.com/@mkotsur/this-is-why-you-should-consider-using-aws-lambda-scala-6b3ea841f8b0).
