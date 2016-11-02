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
