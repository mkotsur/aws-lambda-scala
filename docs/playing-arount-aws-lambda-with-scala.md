# Playing around AWS Lambda with Scala

# Part 1. Handling case classes.

Let's make a Scala handler for AWS Lambda. Don't worry if you don't know what Lambda is: imagine a function which accepts and returns values. Such a function can be conveniently hooked up into the AWS machinery, and triggered [on different events](http://docs.aws.amazon.com/lambda/latest/dg/invoking-lambda-function.html), or used for handling HTTP requests coming through [API gateway](http://docs.aws.amazon.com/lambda/latest/dg/invoking-lambda-function.html#supported-event-source-api-gateway). Let's focus on the HTTP requests accepting and returning JSON.

To define such http handler Lambda means to upload it's code and specify the entry point (e.g. `example.Hello::myHandler`), where myHandler is just a method in [one of 2 forms](http://docs.aws.amazon.com/lambda/latest/dg/java-programming-model-handler-types.html):
 
 **Form 1:**
 
 ```
 OutputType handle(InputType input, Context context) {
    // do stuff
    return someValue;
 }
 ```
 
 `InputType` and `OutputType` here can be either primitive Java types, or POJO classes. That doesn't seem to be very useful if we want to write handler in Scala.
  
  **Form 2:**
  
 ```
 public void handle(InputStream inputStream, OutputStream outputStream, Context context) {
    ...
 }
 ```
 
 Here we can hook up into input and output streams and use any [de]serialization technique we can possibly think of. Let's do it!
  
## Step 0. What does Amazon recommend?

  There is an official "[Writing AWS Lambda Functions in Scala](https://aws.amazon.com/blogs/compute/writing-aws-lambda-functions-in-scala/)" howto, but I found it a bit redundant to explicitly invoke [de]serialization code in every handler, and also Jackson is not something a Scala developer will be proud of adding to the classpath :-)
 
## Step 1. where we support Ping and Pong
 
... so that `OutputType` and `InputType` from Form 1 can be serializable case classes composed of serializable case classes or scala primitives. This is what libraries like [Spray JSON](https://github.com/spray/spray-json) or [Circe](https://github.com/travisbrown/circe) can do out of the box when you add some implicit conversions.
  
For instance, with Circe (more about [encoding and decoding](https://travisbrown.github.io/circe/tut/codec.html)):
  
```scala
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

case class Person(name: String, age: Int)

val person = Person("Tom", 42)
 
println(person.asJson.noSpaces)
// {"name": "Tom", "age": 42}
```

So, for our case it would be nice to be able to define a handler like:

```scala
  class HelloWorldHandler {
  
    case class Ping(msg: String)

    case class Pong(msg: String)
  
    def handle(ping: Ping): Pong = Pong(ping.msg.reverse)
  
  }
```

Or, in other words we need a function which will take "our" friendly handler into AWS's stream-based handler... Something like:

```scala
  type StreamHandler = (InputStream, OutputStream, Context) => Unit
  type ObjectHandler = Ping => Pong

  def objectHandlerToStreamHandler(objectHandler: ObjectHandler): StreamHandler =
    (input: InputStream, output: OutputStream, context: Context) => {
      val jsonString = Source.fromInputStream(input).mkString
      val ping: Ping = magicalFromJson(jsonString)
      val pong: Pong = objectHandler(ping)   
      output.write(magicalToJson(pong).getBytes())
    }
    
```

Now if we mix and match, and add everything to our class, and fill `objectHandlerToStreamHandler` with real implementation, it becomes:

```scala
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe._
// ...
    
    class HelloWorldHandler {
    
    case class Ping(msg: String)
    
    case class Pong(msg: String)
    
    type StreamHandler = (InputStream, OutputStream, Context) => Unit
    
    type ObjectHandler = Ping => Pong
    
    def handle(ping: Ping): Pong = Pong(ping.msg.reverse)
    
    def handleInternal = objectHandlerToStreamHandler(handle)
    
    def objectHandlerToStreamHandler(objectHandler: ObjectHandler): StreamHandler =
    (input: InputStream, output: OutputStream, context: Context) => {
      decode[Ping](Source.fromInputStream(input).mkString)
        .map(objectHandler)
        .map(_.asJson.noSpaces)
        .foreach(jsonStrong => output.write(jsonStrong.getBytes(Charset.defaultCharset())))
    }
  
  }
```

## Step 2. where we support all serializable case classes

Let's get rid of `Ping` and `Pong` in `objectHandlerToStreamHandler` code, and all the boilerplate code from the class with the handler function! May be, inheritance will leave us with the smallest amount of code in the handler class...
 
 ```scala
 import io.circe.generic.auto._
 import io.github.mkotsur.model.{Ping, Pong}
 
 
 class HelloWorldHandler extends JsonHandler[Ping, Pong] {
 
   override def handle(ping: Ping): Pong = Pong(ping.msg.reverse)
 
 }
 ```
 
 And `JsonHandler` will look like this:
 
 ```scala
 import java.io.{InputStream, OutputStream}
 import java.nio.charset.Charset
 
 import com.amazonaws.services.lambda.runtime.Context
 import io.circe._
 import io.circe.parser._
 import io.circe.syntax._
 
 import scala.io.Source
 
 abstract class JsonHandler[I, O](implicit decoder: Decoder[I], encoder: Encoder[O]) {
 
   def handle(x: I): O
 
   def handleInternal = objectHandlerToStreamHandler(handle)
 
   type StreamHandler = (InputStream, OutputStream, Context) => Unit
   type ObjectHandler = I => O
 
   def objectHandlerToStreamHandler(objectHandler: ObjectHandler): StreamHandler =
     (input: InputStream, output: OutputStream, context: Context) => {
       decode[I](Source.fromInputStream(input).mkString)
         .map(objectHandler)
         .map(_.asJson.noSpaces)
         .foreach(jsonStrong => output.write(jsonStrong.getBytes(Charset.defaultCharset())))
     }
 
 }
 ```
 
 The little trick we're using here is passing implicits `implicit decoder: Decoder[I], encoder: Encoder[O]` from `HelloWorldHandler`, where the type arguments are being defined into `JsonHandler`, and that enables methods `decode[I]` and `asJson`. The arguments for those implicit parameters are coming from `io.circe.generic.auto._`.
 
 This might look not very functional, but keep in mind our hard requirement: we have to fulfill the contract of AWS, and this is done in `HelloWorldHandler::handleInternal`. In Part 2 we will see if we can do any better using more advanced usage of type parameters and macros. And in Part 3 we will see how enable [de]serialization of complex classes that can't be picked up by `circe` automatically.
 
 
 More info:
 
 A very similar example/utility library: http://yeghishe.github.io/2016/10/16/writing-aws-lambdas-in-scala.html