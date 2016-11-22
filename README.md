[![Codacy Badge](https://api.codacy.com/project/badge/Grade/0fb7e6e25c1846e3b54f836bbb65a24b)](https://www.codacy.com/app/miccots/aws-lambda-scala?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=mkotsur/aws-lambda-scala&amp;utm_campaign=Badge_Grade)
[![Build Status](https://circleci.com/gh/mkotsur/aws-lambda-scala.svg?&style=shield&circle-token=22c35ff0e9c28f61d483d178f8932c928e47dfc2)](https://circleci.com/gh/mkotsur/aws-lambda-scala)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.mkotsur/aws-lambda-scala_2.12/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.github.mkotsur%22)

Writing a handler for AWS lambda in Scala can be as easy as...

```scala
import io.github.mkotsur.aws.handler.JsonHandler
import io.circe.generic.auto._

case class Ping(inputMsg: String)

case class Pong(outputMsg: String)

class PingPongHandler extends JsonHandler[Ping, Pong] {

  override def handleJson(ping: Ping): Pong = Pong(ping.inputMsg.reverse)

}
```

More documentations and features coming soon...

## Adding to your project

```sbt
libraryDependencies += "io.github.mkotsur" % "aws-lambda-scala_2.12" % "0.0.2"
```