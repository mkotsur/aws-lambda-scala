package io.github.mkotsur.aws.handler

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import io.github.mkotsur.aws.handler.FLambda.logger
import org.slf4j.LoggerFactory

import scala.util.Try

object FLambda {
  //TODO: make sure the logger is max FP optimal
  private val logger = LoggerFactory.getLogger(getClass)
}

abstract class FLambda[F[_], I: CanDecode, O: CanEncode](implicit unwrapper: CanUnwrap[F, O])
    extends RequestStreamHandler {
  type In  = F[I]
  type Out = F[O]

  def handle(i: I, c: Context): Out

  // This function will ultimately be used as the external handler
  final def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit =
    //TODO: decode and encode also using an Effect
    {
      val doneOrDead = for {
        // decode input
        inputF <- CanDecode[I].readStream(input)
        // handle runtime exceptions
        outputF <- Try(handle(inputF, context)).toEither
      } yield // unwrap output
      unwrapper.unwrapAsync(
        outputF, {
          case Right(outputV) =>
            //TODO: refactor
            //TODO: test for decoding error
            //TODO: is it legitimate to throw here?
            Try(CanEncode[O].writeStream(output, Right(outputV), context)).toEither.flatten.left.foreach(e => throw e)
          case Left(e) =>
            logger.error(s"Lambda handler returned a failure-value", e)
            throw e
        }
      )

      doneOrDead.left.foreach(e => throw new java.lang.Error("Uncaught error while executing lambda handler", e))
    }
}
