package io.github.mkotsur.aws.cats_effect

import io.github.mkotsur.aws.handler.CanUnwrap

import scala.concurrent.{ExecutionContext, Future}

package object cats_effect {

  //This unwrapper needs an execution context.
  //TODO: write an implicit not found error message
  implicit def canUnwrapFuture[A](implicit ec: ExecutionContext) = new CanUnwrap[Future, A] {
    override def unwrapAsync(wrapped: Future[A], cb: Either[Throwable, A] => Unit): Unit =
      wrapped.onComplete(t => cb(t.toEither))
  }

}
