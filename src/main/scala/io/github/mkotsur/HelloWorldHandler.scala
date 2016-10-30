package io.github.mkotsur

import io.github.mkotsur.model.{Ping, Pong}

class HelloWorldHandler {

  def handle(ping: Ping): Pong = Pong(ping.msg.reverse)

}
