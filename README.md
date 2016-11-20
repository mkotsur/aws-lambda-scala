
[![Build Status](https://circleci.com/gh/mkotsur/aws-lambda-scala.svg?&style=shield&circle-token=22c35ff0e9c28f61d483d178f8932c928e47dfc2)](https://circleci.com/gh/mkotsur/aws-lambda-scala) 
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