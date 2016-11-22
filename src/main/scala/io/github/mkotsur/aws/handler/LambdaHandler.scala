package io.github.mkotsur.aws.handler

import java.io.{InputStream, OutputStream}
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

import com.amazonaws.services.lambda.runtime.Context
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.github.mkotsur.aws.handler.LambdaHandler.{CanDecode, CanEncode, ObjectHandler, ReadStream, WriteStream}

import scala.io.Source

object LambdaHandler {

  type ReadStream[I] = InputStream => Either[Error, I]

  // Might look redundant extracting this as a separate type,
  // but it will be much easier to perform refactoring,
  // like to return Either[Error, O] as output...
  type ObjectHandler[I, O] = I => O

  type WriteStream[O] = (OutputStream, Either[Error, O], Context) => Unit

  trait CanDecode[I] {
    def readStream: InputStream => Either[Error, I]
  }

  trait CanEncode[O] {
    def writeStream(output: OutputStream, o: Either[Error, O], context: Context): Unit
  }

  implicit def canDecodeString = new CanDecode[String] {
    override def readStream: (InputStream) => Either[Error, String] = is => Right(Source.fromInputStream(is).mkString)
  }

  implicit def canEncodeString = new CanEncode[String] {
    override def writeStream(output: OutputStream, o: Either[Error, String], context: Context): Unit = o match {
      case Left(e) => context.getLogger.log(s"Error: ${e.getMessage}")
      case Right(s) => output.write(s.getBytes(Charset.defaultCharset()))
    }
  }

  implicit def canDecodeCaseClasses[T](implicit decoder: Decoder[T]) = new CanDecode[T] {
    override def readStream: (InputStream) => Either[Error, T] = is => decode[T](Source.fromInputStream(is).mkString)
  }

  implicit def canEncodeCaseClasses[T](implicit encoder: Encoder[T]) = new CanEncode[T] {
    override def writeStream(output: OutputStream, o: Either[Error, T], context: Context): Unit = {
      o.map(_.asJson.noSpaces) match {
        case Left(error) =>
          context.getLogger.log(s"Error: ${error.getMessage}")
          throw error
        case Right(jsonString) =>
          output.write(jsonString.getBytes(UTF_8))
      }
    }
  }

}

abstract class LambdaHandler[I, O](implicit canDecode: CanDecode[I], canEncode: CanEncode[O]) {

  private type StreamHandler = (InputStream, OutputStream, Context) => Unit

  // This method should be overriden
  protected def handle(x: I): O

  // This function will ultimately be used as the external handler
  final def handle(i: InputStream, o: OutputStream, c: Context): Unit =
    objectHandlerToStreamHandler(canDecode.readStream, handle, canEncode.writeStream)(i, o, c)

  protected def objectHandlerToStreamHandler(readStream: ReadStream[I],
                                             objectHandler: ObjectHandler[I, O],
                                             writeStream: WriteStream[O]): StreamHandler =
    (input: InputStream, output: OutputStream, context: Context) =>
      readStream(input)
        .map(objectHandler)
        .map(o => {
          writeStream(output, Right(o), context)
          output.close()
        })
}
