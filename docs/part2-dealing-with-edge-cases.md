# Playing around AWS Lambda with Scala

# Part 2. Dealing with edge cases

## Supporting raw types

The next thing I had to think about was that not always you need to have JSON for input and output. Sometimes you know that it's a string. It would be cool if our JSON handler could detect that in some cases it does not need to run the input (or output) through the jsonification pipeline, and just leave things as it.
 
 ```scala
 import io.github.mkotsur.aws.handler.JsonHandler
 import io.circe.generic.auto._

case class Pong(outputMsg: String)
 
class StringPongHandler extends JsonHandler[String, Pong] {
 override def handleJson(x: String): Pong = Pong(x.capitalize)
}
 ```
 
or even:
 
```scala
import io.github.mkotsur.aws.handler.JsonHandler
import io.circe.generic.auto._
import java.io.InputStream

case class Pong(outputMsg: String)
 
class StringPongHandler extends JsonHandler[InputStream, Pong] {
 override def handleJson(is: InputStream): Pong = {
  // do something with the stream
  Pong("something")
 }
}
 ``` 
 
 