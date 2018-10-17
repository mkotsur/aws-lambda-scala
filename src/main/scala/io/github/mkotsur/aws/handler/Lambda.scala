package io.github.mkotsur.aws.handler

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.Context
import io.circe.generic.auto._
import io.github.mkotsur.aws.codecs._
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}
import org.slf4j.LoggerFactory

import scala.language.{higherKinds, postfixOps}
import scala.util.{Failure, Success, Try}

object Lambda extends AllCodec with ProxyRequestCodec {

  type Handle[I, O] = (I, Context) => HandleResult[O]
  type HandleResult[O] = Either[Throwable, O]
  type Proxy[I, O] = Lambda[ProxyRequest[I], ProxyResponse[O]]

  object Proxy {
    type Handle[I, O] = (ProxyRequest[I], Context) => HandleResult[O]
    type HandleResult[O] = Either[Throwable, ProxyResponse[O]]

    private type CanDecodeProxyRequest[A] = CanDecode[ProxyRequest[A]]
    private type CanEncodeProxyRequest[A] = CanEncode[ProxyResponse[A]]

    def instance[I: CanDecodeProxyRequest, O: CanEncodeProxyRequest](doHandle: Proxy.Handle[I, O]): Lambda[ProxyRequest[I], ProxyResponse[O]] = {
      new Lambda.Proxy[I, O] {
        override protected def handle(i: ProxyRequest[I], c: Context) = doHandle(i, c)
      }
    }
  }

  def instance[I: CanDecode, O: CanEncode](doHandle: Handle[I, O]) = {
    new Lambda[I, O] {
      override protected def handle(i: I, c: Context): Either[Throwable, O] = {
        super.handle(i, c)
        doHandle(i, c)
      }
    }
  }

  type ReadStream[I] = InputStream => Either[Throwable, I]
  type ObjectHandler[I, O] = I => Either[Throwable, O]
  type WriteStream[O] = (OutputStream, Either[Throwable, O], Context) => Either[Throwable, Unit]

  private val logger = LoggerFactory.getLogger(getClass)

}

abstract class Lambda[I: CanDecode, O: CanEncode] {

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
    val read = implicitly[CanDecode[I]].readStream(input)
    val handled = read.flatMap { input =>
      Try(handle(input, context)) match {
        case Success(v) => v
        case Failure(e) =>
          Lambda.logger.error(s"Error while executing lambda handler: ${e.getMessage}", e)
          Left(e)
      }
    }
    val written = implicitly[CanEncode[I]].writeStream(output, handled, context)
    output.close()
    written.left.foreach(e => throw e)
  }

}
