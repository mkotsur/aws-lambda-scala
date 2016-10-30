package io.github.mkotsur

import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import java.io.{InputStream, OutputStream}
import java.nio.charset.Charset

import com.amazonaws.services.lambda.runtime.Context
import io.github.mkotsur.model.{Ping, Pong}

import scala.io.Source


class HelloWorldHandler {

  def handle(ping: Ping): Pong = Pong(ping.msg.reverse)

  def handleInternal(input: InputStream, output: OutputStream, ctx: Context = null): Unit = {
    decode[Ping](Source.fromInputStream(input).mkString)
      .map(handle)
      .map(_.asJson.noSpaces)
      .foreach(jsonStrong => output.write(jsonStrong.getBytes(Charset.defaultCharset())))
  }

}
