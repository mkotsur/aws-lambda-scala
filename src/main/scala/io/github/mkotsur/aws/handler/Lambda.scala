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
import org.slf4j.LoggerFactory
import shapeless.Generic

import scala.io.Source
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object Lambda {

  type Proxy[I, O] = Lambda[ProxyRequest[I], ProxyResponse[O]]

  type ReadStream[I] = InputStream => Either[Throwable, I]

  type ObjectHandler[I, O] = I => Either[Throwable, O]

  type WriteStream[O] = (OutputStream, Either[Throwable, O], Context) => Either[Throwable, Unit]

  private val logger = LoggerFactory.getLogger(getClass)

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

  implicit def canEncodeProxyResponse[T](implicit canEncode: Encoder[T]) = CanEncode.instance[ProxyResponse[T]](
    (output, proxyResponseEither, _) => {

      val response = proxyResponseEither match {
        case Right(proxyResponse) =>
          val encodedBodyOption = proxyResponse.body.map(bodyObject => bodyObject.asJson.noSpaces)
          ProxyResponse[String](
            proxyResponse.statusCode,
            proxyResponse.headers,
            encodedBodyOption
          )
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

abstract class Lambda[I, O](implicit canDecode: CanDecode[I], canEncode: CanEncode[O]) {

  // Either of the following two methods should be overridden
  protected def handle(i: I, c: Context): Either[Throwable, O] = handle(i)

  @deprecated(message = "This method is deprecated. " +
                "Please implement the handle, which takes context as a parameter. " +
                "See #4 for more details.",
              "")
  protected def handle(i: I): Either[Throwable, O] =
    Left(new NotImplementedError("Please implement the method handle(i: I, c: Context)"))

  // This function will ultimately be used as the external handler
  final def handle(input: InputStream, output: OutputStream, context: Context): Unit = {
    val read = canDecode.readStream(input)
    val handled = read.flatMap { input =>
      Try(handle(input, context)) match {
        case Success(v) => v
        case Failure(e) =>
          Lambda.logger.error(s"Error while executing lambda handler: ${e.getMessage}", e)
          Left(e)
      }
    }
    val written = canEncode.writeStream(output, handled, context)
    output.close()
    written.left.foreach(e => throw e)
  }

}
