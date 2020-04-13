package io.github.mkotsur.aws

import cats.effect.IO
import cats.effect._
import io.github.mkotsur.aws.handler.CanUnwrap

package object cats_effect {

  implicit def canUnwrapIO[A] = new CanUnwrap[IO, A] {
    override def unwrapAsync(wrapped: IO[A], cb: Either[Throwable, A] => Unit): Unit =
      wrapped.unsafeRunAsync(cb)
  }

}
