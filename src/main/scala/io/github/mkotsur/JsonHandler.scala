package io.github.mkotsur

import java.io.{InputStream, OutputStream}
import java.nio.charset.Charset

import com.amazonaws.services.lambda.runtime.Context
import io.circe._
import io.circe.parser._
import io.circe.syntax._

import scala.io.Source

abstract class JsonHandler[I, O](implicit decoder: Decoder[I], encoder: Encoder[O]) {

  def handle(x: I): O

  def handleInternal(input: InputStream, output: OutputStream, ctx: Context = null): Unit = {
    decode[I](Source.fromInputStream(input).mkString)
      .map(handle)
      .map(_.asJson.noSpaces)
      .foreach(jsonStrong => output.write(jsonStrong.getBytes(Charset.defaultCharset())))
  }

}
