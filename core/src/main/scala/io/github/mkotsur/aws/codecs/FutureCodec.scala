package io.github.mkotsur.aws.codecs

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

import io.circe.Encoder
import io.github.mkotsur.aws.handler.CanEncode
import io.github.mkotsur.aws.proxy.ProxyResponse
import io.circe.generic.auto._
import io.circe.syntax._
import cats.syntax.either.catsSyntaxEither

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

}
