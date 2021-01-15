package io.github.mkotsur.aws.codecs

import java.io.ByteArrayInputStream
import cats.syntax.either.catsSyntaxEither
import io.circe.{Decoder, Json}
import io.circe.generic.auto._
import io.github.mkotsur.aws.handler.CanDecode
import io.github.mkotsur.aws.proxy.ApiProxyRequest
import shapeless.Generic

import scala.language.{higherKinds, postfixOps}

private[aws] trait ProxyRequestCodec extends AllCodec with FutureCodec {

  /**
    * This is a transformer between case classes and their generic representations [shapeless.HList].
    * Please check Shapeless guide (e.g. https://github.com/underscoreio/shapeless-guide) for more details.
    */
  implicit def canDecodeProxyRequest[T: CanDecode, C: Decoder] =
    CanDecode.instance[ApiProxyRequest[T, C]] { is =>
      {
        def extractBody(s: ApiProxyRequest[String, C]) = s.body match {
          case Some(bodyString) =>
            CanDecode[T].readStream(new ByteArrayInputStream(bodyString.getBytes)).map(Option.apply)
          case None => Right(None)
        }

        def produceProxyResponse(decodedRequestString: ApiProxyRequest[String, C], bodyOption: Option[T]) = {
          val reqList = Generic[ApiProxyRequest[String, C]].to(decodedRequestString)
          Generic[ApiProxyRequest[T, C]].from((bodyOption :: reqList.reverse.tail).reverse)
        }

        for (decodedRequest$String <- CanDecode[ApiProxyRequest[String, C]].readStream(is);
             decodedBodyOption     <- extractBody(decodedRequest$String))
          yield produceProxyResponse(decodedRequest$String, decodedBodyOption)
      }
    }

}
