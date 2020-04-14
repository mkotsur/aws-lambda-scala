package io.github.mkotsur.aws.handler

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import io.github.mkotsur.aws.codecs._
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}
import org.slf4j.LoggerFactory
import io.github.mkotsur.aws.handler.Lambda.HandleResult

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object Lambda extends AllCodec with ProxyRequestCodec {

  private type Handle[I, O]    = (I, Context) => HandleResult[O]
  private type HandleResult[O] = Either[Throwable, O]
  type Proxy[I, O]             = Lambda[ProxyRequest[I], ProxyResponse[O]]

  object Proxy {
    type Handle[I, O]    = (ProxyRequest[I], Context) => HandleResult[O]
    type HandleResult[O] = Either[Throwable, ProxyResponse[O]]

    private type CanDecodeProxyRequest[A] = CanDecode[ProxyRequest[A]]
    private type CanEncodeProxyRequest[A] = CanEncode[ProxyResponse[A]]

    def instance[I: CanDecodeProxyRequest, O: CanEncodeProxyRequest](
        doHandle: Proxy.Handle[I, O]): Lambda[ProxyRequest[I], ProxyResponse[O]] =
      new Lambda.Proxy[I, O] {
        override protected def handle(i: ProxyRequest[I], c: Context) = doHandle(i, c)
      }
  }

  def instance[I: CanDecode, O: CanEncode](doHandle: Handle[I, O]) =
    new Lambda[I, O] {
      override protected def handle(i: I, c: Context): Either[Throwable, O] = {
        super.handle(i, c)
        doHandle(i, c)
      }
    }

  private val logger = LoggerFactory.getLogger(getClass)

}

abstract class Lambda[I: CanDecode, O: CanEncode] extends RequestStreamHandler {

  /**
    * Either of the following two methods should be overridden,
    * if ths one is overriden, its implementation will be called from `handleRequest`, and `handle(i: I)` will never be used..
    * if the `handle(i: I)` is overriden, this method will delegate to that one and NotImplementedError will not occur.
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
