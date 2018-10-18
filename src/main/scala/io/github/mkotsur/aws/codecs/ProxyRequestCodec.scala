package io.github.mkotsur.aws.codecs

import java.io.ByteArrayInputStream
import cats.syntax.either.catsSyntaxEither
import io.circe.generic.auto._
import io.github.mkotsur.aws.handler.CanDecode
import io.github.mkotsur.aws.proxy.ProxyRequest
import shapeless.Generic

import scala.language.{higherKinds, postfixOps}

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

}
