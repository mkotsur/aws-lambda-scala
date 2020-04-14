package io.github.mkotsur.aws.codecs

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.Charset

import io.circe.generic.auto._
import io.github.mkotsur.aws.handler.{CanDecode, CanEncode}
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}
import shapeless.Generic

import scala.language.postfixOps

private[aws] trait ProxyRequestCodec extends AllCodec with FutureCodec {

  /**
    * This is a transformer between case classes and their generic representations [shapeless.HList].
    * Please check Shapeless guide (e.g. https://github.com/underscoreio/shapeless-guide) for more details.
    */
  def GenericProxyRequestOf[T] = shapeless.Generic[ProxyRequest[T]]

  implicit def canDecodeProxyRequest[T](implicit canDecode: CanDecode[T]) = CanDecode.instance[ProxyRequest[T]] { is =>
    {
      def extractBody(s: ProxyRequest[String]) = s.body match {
        case Some(bodyString) => canDecode.readStream(new ByteArrayInputStream(bodyString.getBytes)).map(Option.apply)
        case None             => Right(None)
      }

      def produceProxyResponse(decodedRequestString: ProxyRequest[String], bodyOption: Option[T]) = {
        val reqList = Generic[ProxyRequest[String]].to(decodedRequestString)
        Generic[ProxyRequest[T]].from((bodyOption :: reqList.reverse.tail).reverse)
      }

      for (decodedRequest$String <- CanDecode[ProxyRequest[String]].readStream(is);
           decodedBodyOption     <- extractBody(decodedRequest$String))
        yield produceProxyResponse(decodedRequest$String, decodedBodyOption)
    }
  }

  implicit def canEncodeProxyResponse[T](implicit canEncode: CanEncode[T]) = CanEncode.instance[ProxyResponse[T]](
    (output, proxyResponseEither, ctx) => {

      def writeBody(bodyOption: Option[T]): Either[Throwable, Option[String]] =
        bodyOption match {
          case None => Right(None)
          case Some(body) =>
            val os     = new ByteArrayOutputStream()
            val result = canEncode.writeStream(os, Right(body), ctx)
            os.close()
            result.map(_ => Some(os.toString()))
        }

      val proxyResponseOrError = for {
        proxyResponse <- proxyResponseEither
        bodyOption    <- writeBody(proxyResponse.body)
      } yield
        ProxyResponse[String](
          proxyResponse.statusCode,
          proxyResponse.headers,
          bodyOption
        )

      val response = proxyResponseOrError match {
        case Right(proxyResponse) =>
          proxyResponse
        case Left(e) =>
          ProxyResponse[String](
            500,
            Some(Map("Content-Type" -> s"text/plain; charset=${Charset.defaultCharset().name()}")),
            Some(e.getMessage)
          )
      }

      import io.circe.generic.auto._
      import io.circe.syntax._
      output.write(response.asJson.noSpaces.getBytes)

      Right(())
    }
  )

}
