package io.github.mkotsur.aws.handler

import java.io.{ByteArrayInputStream, InputStream, OutputStream}
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

import com.amazonaws.services.lambda.runtime.Context
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.mkotsur.aws.handler.LambdaHandler.{CanDecode, CanEncode, ObjectHandler, ReadStream, WriteStream}
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}

import scala.io.Source
import scala.util.Try

object LambdaHandler {

  type ReadStream[I] = InputStream => Either[Throwable, I]

  // Might look redundant extracting this as a separate type,
  // but it will be much easier to perform refactoring,
  // like to return Either[Error, O] as output...
  type ObjectHandler[I, O] = I => O

  type WriteStream[O] = (OutputStream, Either[Throwable, O], Context) => Unit

  trait CanDecode[I] {
    def readStream: ReadStream[I]
  }

  trait CanEncode[O] {
    def writeStream: WriteStream[O]
  }

  /**
    * The implementation of 2 following methods should most definitely be rewritten for it's ugly as sin.
    */
  object proxy {

    type ProxyRequest$String = ProxyRequest[String]
    type ProxyResponse$String = ProxyResponse[String]

    implicit def canDecodeProxyRequest[T](implicit decoderT: CanDecode[T]) = new CanDecode[ProxyRequest[T]] {
      override def readStream = is => {

        val eitherPRS: Either[Throwable, ProxyRequest$String] = decode[ProxyRequest$String](Source.fromInputStream(is).mkString)

        eitherPRS.flatMap(prs => {

          val optionEither = prs.body
            .map(bodyString => new ByteArrayInputStream(bodyString.getBytes))
            .map(decoderT.readStream)

          val bodyEitherOption: Either[Throwable, Option[T]] = optionEither match {
            case None => Right(None)
            case Some(Left(l)) => Left(l)
            case Some(Right(v)) => Right[Throwable, Option[T]](Some(v))
          }

          val newEither: Either[Throwable, ProxyRequest[T]] = bodyEitherOption.flatMap(bo => Right(ProxyRequest[T](
            prs.path,
            prs.httpMethod,
            prs.headers,
            prs.queryStringParameters,
            prs.stageVariables,
            bo,
            prs.requestContext
          )))

          newEither
        })

      }
    }

    implicit def canEncodeProxyResponse[T](implicit canEncode: Encoder[T]) = new CanEncode[ProxyResponse[T]] {
      override def writeStream = (output, o, context) => {
        o.foreach { proxyResponse =>
          val encodedBodyOption = proxyResponse.body.map(bodyObject => bodyObject.asJson.noSpaces)

          ProxyResponse[String](
            proxyResponse.statusCode, proxyResponse.headers,
            encodedBodyOption
          ).asJson.noSpaces
        }
      }
    }
  }

  object string {
    implicit def canDecodeString = new CanDecode[String] {
      override def readStream = is => Right(Source.fromInputStream(is).mkString)
    }

    implicit def canEncodeString = new CanEncode[String] {
      override def writeStream: WriteStream[String] = (output, o, context) => o match {
        case Left(e) => context.getLogger.log(s"Error: ${e.getMessage}")
        case Right(s) => output.write(s.getBytes(Charset.defaultCharset()))
      }
    }
  }

  implicit def canDecodeCaseClasses[T](implicit decoder: Decoder[T]) = new CanDecode[T] {
    override def readStream = is => decode[T](Source.fromInputStream(is).mkString)
  }

  implicit def canEncodeCaseClasses[T](implicit encoder: Encoder[T]) = new CanEncode[T] {
    override def writeStream: WriteStream[T] = (output, o, context) => {
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
    (input: InputStream, output: OutputStream, context: Context) => {
      val readEither = readStream(input)

      readEither match {
        case Left(error) => throw error
        case Right(obj) =>
          writeStream(output, Try(objectHandler(obj)).toEither, context)
          output.close()
      }
    }
}
