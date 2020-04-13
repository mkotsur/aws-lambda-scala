package foo

import cats.effect.IO
import io.github.mkotsur.aws.handler.FLambda

case class Ping(inputMsg: String)

case class Pong(outputMsg: String)
import com.amazonaws.services.lambda.runtime.Context
import io.circe.generic.auto._
import io.github.mkotsur.aws.cats_effect._
import io.github.mkotsur.aws.handler.Lambda._

class FooHandler extends FLambda[IO, Ping, Pong] {

  override def handle(ping: Ping, context: Context): IO[Pong] =
    IO(Pong(ping.inputMsg.reverse))

}
