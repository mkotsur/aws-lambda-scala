package io.github.mkotsur

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.Context
import io.github.mkotsur.model.{Ping, Pong}


class HelloWorldHandler {

  def handle(ping: Ping): Pong = Pong(ping.msg.reverse)

  def handleInternal(input: InputStream, output: OutputStream, ctx: Context = null): Unit = {
    // convert `input` into Pong
    // apply `handle()`
    // convert the result into JSON
    // write that into `output`
  }

}
