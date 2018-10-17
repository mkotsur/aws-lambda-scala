package io.github.mkotsur.aws.codecs

import java.nio.charset.StandardCharsets.UTF_8

import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import io.github.mkotsur.aws.handler.{CanDecode, CanEncode}

import scala.io.Source
import scala.reflect.ClassTag

private[aws] trait AllCodec {

  implicit def canDecodeAll[T: ClassTag](implicit decoder: Decoder[T]) =
    CanDecode.instance[T](
      implicitly[ClassTag[T]] match {
        case ct if ct.runtimeClass == classOf[String] =>
          is =>
            Right(Source.fromInputStream(is).mkString.asInstanceOf[T])
        case _ =>
          is =>
            decode[T](Source.fromInputStream(is).mkString)
      }
    )

  implicit def canEncodeAll[T: ClassTag](implicit encoder: Encoder[T]) = CanEncode.instance[T](
    implicitly[ClassTag[T]] match {
      case ct if ct.runtimeClass == classOf[String] =>
        (output, handledEither, _) =>
          handledEither.map { s =>
            output.write(s.asInstanceOf[String].getBytes)
          }
      case _ =>
        (output, handledEither, _) =>
          handledEither map { handled =>
            val jsonString = handled.asJson.noSpaces
            output.write(jsonString.getBytes(UTF_8))
          }
    }
  )
}
