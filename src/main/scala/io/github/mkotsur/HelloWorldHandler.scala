package io.github.mkotsur


import io.circe.generic.auto._
import io.github.mkotsur.model.{Ping, Pong}


class HelloWorldHandler extends JsonHandler[Ping, Pong] {

  override def handle(ping: Ping): Pong = Pong(ping.msg.reverse)

}
