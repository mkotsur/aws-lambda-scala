package io.github.mkotsur.aws.codecs

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

import io.circe.Encoder
import io.github.mkotsur.aws.handler.CanEncode
import io.github.mkotsur.aws.proxy.ProxyResponse
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

private[aws] trait FutureCodec {
  implicit def canEncodeFuture[I: Encoder](implicit canEncode: Encoder[I]) =
    CanEncode.instance[Future[I]]((os, responseEither, ctx) => {
      (for {
        response     <- responseEither.toTry
        futureResult <- Try(Await.result(response, ctx.getRemainingTimeInMillis millis))
        json         <- Try(canEncode(futureResult).noSpaces.getBytes)
        _            <- Try(os.write(json))
      } yield {
        ()
      }) match {
        case Success(v) => Right(v)
        case Failure(e) => Left(e)
      }
    })

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

      val proxyResposeOrError = for {
        proxyResponse <- proxyResponseEither
        bodyOption    <- writeBody(proxyResponse.body)
      } yield
        ProxyResponse[String](
          proxyResponse.statusCode,
          proxyResponse.headers,
          bodyOption
        )

      val response = proxyResposeOrError match {
        case Right(proxyRespose) =>
          proxyRespose
        case Left(e) =>
          ProxyResponse[String](
            500,
            Some(Map("Content-Type" -> s"text/plain; charset=${Charset.defaultCharset().name()}")),
            Some(e.getMessage)
          )
      }

      output.write(response.asJson.noSpaces.getBytes)

      Right(())
    }
  )
}
