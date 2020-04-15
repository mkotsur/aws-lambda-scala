package io.github.mkotsur.aws.handler

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.language.postfixOps
import scala.util.Try

abstract class FLambda[F[_], I: CanDecode, O: CanEncode: CanUnwrap[F, *]]
    extends RequestStreamHandler {
  type In  = F[I]
  type Out = F[O]

  def handle(i: I, c: Context): Out

  // This function will ultimately be used as the external handler
  final def handleRequest(input: InputStream,
                          output: OutputStream,
                          context: Context): Unit =
    //TODO: decode and encode also using an Effect
    {
      val doneOrDead = for {
        // decode input
        inputF <- CanDecode[I].readStream(input)
        // handle runtime exceptions
        outputF <- Try(handle(inputF, context)).toEither
      } yield { // unwrap output
        //TODO: change `unwrapAsync` signature to return Future???
        val unwrappedPromise: Promise[O] = Promise()
        CanUnwrap[F, O].unwrapAsync(
          outputF,
          result => {
            CanEncode[O].writeStream(output, result, context)
            unwrappedPromise.complete(result.toTry)
          }
        )

        // TODO: configurable context ???
        val finallyDoneFuture = {
          import scala.concurrent.ExecutionContext.Implicits.global
          unwrappedPromise.future
            .recoverWith(e => Future.failed(LambdaFailureException(e)))
        }

        // Throws
        Await.result(finallyDoneFuture, context.getRemainingTimeInMillis millis)
      }

      doneOrDead.left.foreach(
        e =>
          throw new java.lang.Error(
            "Uncaught error while executing lambda handler",
            e))
    }
}
