package io.github.mkotsur.aws.handler

import java.io.{ByteArrayInputStream, InputStream, OutputStream}
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

import com.amazonaws.services.lambda.runtime.Context
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}
import org.apache.http.HttpStatus
import shapeless.Generic

import scala.io.Source
import scala.reflect.ClassTag

object LambdaHandler {

  type Proxy[I, O] = LambdaHandler[ProxyRequest[I], ProxyResponse[O]]

  type ReadStream[I] = InputStream => Either[Throwable, I]

  type ObjectHandler[I, O] = I => Either[Throwable, O]

  type WriteStream[O] = (OutputStream, Either[Throwable, O], Context) => Either[Throwable, Unit]

  object proxy {

    // We could definitely do something smart with these 2 types,
    // but let's keep them here just for now to have things under control.
    type ProxyRequest$String = ProxyRequest[String]
    type ProxyResponse$String = ProxyResponse[String]

    implicit def canDecodeProxyRequest[T](implicit decoderT: CanDecode[T]) = CanDecode.instance[ProxyRequest[T]] {
      is => {
        def extractBody(s: ProxyRequest$String) = s.body match {
          case Some(bodyString) => decoderT.readStream(new ByteArrayInputStream(bodyString.getBytes)).map(Option.apply)
          case None => Right(None)
        }

        def produceProxyResponse(decodedRequest$String: ProxyRequest$String, bodyOption: Option[T]) = {
          val reqList = Generic[ProxyRequest$String].to(decodedRequest$String)
          Generic[ProxyRequest[T]].from((bodyOption :: reqList.reverse.tail).reverse)
        }

        for (
          decodedRequest$String <- decode[ProxyRequest$String](Source.fromInputStream(is).mkString);
          decodedBodyOption <- extractBody(decodedRequest$String)
        ) yield produceProxyResponse(decodedRequest$String, decodedBodyOption)
      }
    }

    implicit def canEncodeProxyResponse[T](implicit canEncode: Encoder[T]) = CanEncode.instance[ProxyResponse[T]](
      (output, proxyResponseEither, _) => {

        val response = proxyResponseEither match {
          case Right(proxyResponse) =>
            val encodedBodyOption = proxyResponse.body.map(bodyObject => bodyObject.asJson.noSpaces)
            ProxyResponse[String](
              proxyResponse.statusCode, proxyResponse.headers,
              encodedBodyOption
            )
          case Left(e) =>
            ProxyResponse[String](
              HttpStatus.SC_INTERNAL_SERVER_ERROR,
              Some(Map("Content-Type" -> s"text/plain; charset=${Charset.defaultCharset().name()}")),
              Some(e.getMessage)
            )
        }

        output.write(response.asJson.noSpaces.getBytes)

        Right(())
      }
    )
  }

  private val classOfString = classOf[String]

  implicit def canDecodeAll[T: ClassTag](implicit decoder: Decoder[T]) =
    CanDecode.instance[T](
      implicitly[ClassTag[T]].runtimeClass match {
        case `classOfString` => is => Right(Source.fromInputStream(is).mkString.asInstanceOf[T])
        case _ => is => decode[T](Source.fromInputStream(is).mkString)
      }
    )

  implicit def canEncodeAll[T: ClassTag](implicit encoder: Encoder[T]) = CanEncode.instance[T](
    implicitly[ClassTag[T]].runtimeClass match {
      case `classOfString` => (output, handledEither, _) =>
        handledEither.map { s => output.write(s.asInstanceOf[String].getBytes) }
      case _ =>
        (output, handledEither, _) => handledEither map { handled =>
          val jsonString = handled.asJson.noSpaces
          output.write(jsonString.getBytes(UTF_8))
        }
    }
  )

}

abstract class LambdaHandler[I, O](implicit canDecode: CanDecode[I], canEncode: CanEncode[O]) {

  private type StreamHandler = (InputStream, OutputStream, Context) => Unit

  // This method should be overriden
  protected def handle(i: I): Either[Throwable, O]

  // This function will ultimately be used as the external handler
  final def handle(input: InputStream, output: OutputStream, context: Context): Unit = {
    val read = canDecode.readStream(input)
    val handled = read.flatMap(handle)
    val written = canEncode.writeStream(output, handled, context)
    output.close()
    written.left.foreach(e => throw e)
  }

}
