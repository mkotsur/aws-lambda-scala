package io.github.mkotsur.aws.handler

import java.io.{InputStream, OutputStream}
import java.nio.charset.StandardCharsets.UTF_8

import com.amazonaws.services.lambda.runtime.Context
import io.circe._
import io.circe.parser._
import io.circe.syntax._

import scala.io.Source

abstract class JsonHandler[I, O](implicit decoder: Decoder[I], encoder: Encoder[O]) {

  def handleJson(x: I): O

  def handle(i: InputStream, o: OutputStream, c: Context): Unit =
    objectHandlerToStreamHandler(handleJson)(i, o, c)

  type StreamHandler = (InputStream, OutputStream, Context) => Unit
  type ObjectHandler = I => O

  protected def objectHandlerToStreamHandler(objectHandler: ObjectHandler): StreamHandler =
    (input: InputStream, output: OutputStream, context: Context) =>
      decode[I](Source.fromInputStream(input).mkString)
        .map(objectHandler)
        .map(_.asJson.noSpaces) match {
        case Left(error) =>
          context.getLogger.log(s"Error: ${error.getMessage}")
          throw error
        case Right(jsonString) =>
          output.write(jsonString.getBytes(UTF_8))
          output.close()
      }
}
