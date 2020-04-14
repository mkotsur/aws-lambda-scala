package io.github.mkotsur.aws.handler

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}

import scala.util.Try

abstract class FLambda[F[_], I: CanDecode, O: CanEncode: CanUnwrap[F, *]] extends RequestStreamHandler {
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
      CanUnwrap[F, O].unwrapAsync(
        outputF,
        result => {
          CanEncode[O].writeStream(output, result, context)
          result.left.foreach(e => throw new java.lang.Error("The returned value was unsuccessful", e))
        }
      )

      doneOrDead.left.foreach(e => throw new java.lang.Error("Uncaught error while executing lambda handler", e))
    }
}
