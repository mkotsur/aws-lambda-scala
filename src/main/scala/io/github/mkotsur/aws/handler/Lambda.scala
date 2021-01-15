package io.github.mkotsur.aws.handler

import java.io.{InputStream, OutputStream}
import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import io.circe.generic.auto._
import io.github.mkotsur.aws.codecs._
import io.github.mkotsur.aws.proxy.{ApiProxyRequest, ProxyResponse}
import org.slf4j.LoggerFactory
import cats.syntax.either.catsSyntaxEither
import io.circe.Json
import io.github.mkotsur.aws.handler.Lambda.HandleResult

import scala.language.{higherKinds, postfixOps}
import scala.util.{Failure, Success, Try}

object Lambda extends AllCodec with ProxyRequestCodec {

  type Handle[I, O]      = (I, Context) => HandleResult[O]
  type HandleResult[O]   = Either[Throwable, O]
  type ApiProxy[I, C, O] = Lambda[ApiProxyRequest[I, C], ProxyResponse[O]]

  object Proxy {
    type Handle[I, C, O] = (ApiProxyRequest[I, C], Context) => HandleResult[O]
    type HandleResult[O] = Either[Throwable, ProxyResponse[O]]

    private type CanEncodeProxyResponse[A] = CanEncode[ProxyResponse[A]]

    def instance[I: CanDecode, C: CanDecode, O: CanEncodeProxyResponse](doHandle: Proxy.Handle[I, C, O])(
        implicit canDecodeFullReq: CanDecode[ApiProxyRequest[I, C]]): Lambda[ApiProxyRequest[I, C], ProxyResponse[O]] =
      new Lambda.ApiProxy[I, C, O] {
        override protected def handle(i: ApiProxyRequest[I, C], c: Context) = doHandle(i, c)
      }
  }

  def instance[I: CanDecode, O: CanEncode](doHandle: Handle[I, O]) =
    new Lambda[I, O] {
      override protected def handle(i: I, c: Context): Either[Throwable, O] = {
        super.handle(i, c)
        doHandle(i, c)
      }
    }

  type ReadStream[I]       = InputStream => Either[Throwable, I]
  type ObjectHandler[I, O] = I => Either[Throwable, O]
  type WriteStream[O]      = (OutputStream, Either[Throwable, O], Context) => Either[Throwable, Unit]

  private val logger = LoggerFactory.getLogger(getClass)

}

abstract class Lambda[I: CanDecode, O: CanEncode] extends RequestStreamHandler {

  /**
    * Either of the following two methods should be overridden,
    * if ths one is overridden, its implementation will be called from `handleRequest`, and `handle(i: I)` will never be used..
    * if the `handle(i: I)` is overridden, this method will delegate to that one and NotImplementedError will not occur.
    */
  protected def handle(i: I, c: Context): Either[Throwable, O] = handle(i)

  protected def handle(i: I): HandleResult[O] =
    Left(new NotImplementedError("Please implement the method handle(i: I, c: Context)"))

  /**
    * For backwards compatibility and naming consistency
    */
  final def handle(input: InputStream, output: OutputStream, context: Context): Unit =
    handleRequest(input, output, context)

  // This function will ultimately be used as the external handler
  final def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    val read = implicitly[CanDecode[I]].readStream(input)
    val handled = read.flatMap { input =>
      Try(handle(input, context)) match {
        case Success(v) => v
        case Failure(e) =>
          Lambda.logger.error(s"Error while executing lambda handler: ${e.getMessage}", e)
          Left(e)
      }
    }
    val written = implicitly[CanEncode[O]].writeStream(output, handled, context)
    output.close()
    written.left.foreach(e => throw e)
  }

}
