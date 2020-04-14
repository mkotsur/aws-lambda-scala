package foo

import cats.effect.IO

case class Ping(inputMsg: String)

case class Pong(outputMsg: String)
import com.amazonaws.services.lambda.runtime.Context
import io.circe.generic.auto._
import io.github.mkotsur.aws.cats_effect.IOLambda
import io.github.mkotsur.aws.handler.Lambda._

class PingPongHandler extends IOLambda[Ping, Pong] {

  override def handle(ping: Ping, context: Context): IO[Pong] =
    IO(Pong(ping.inputMsg.reverse))

}
