package io.github.mkotsur.aws.eff

import io.github.mkotsur.aws.handler.CanUnwrap

package object either {

  type ThrowableOr[A] = Either[Throwable, A]

  implicit def canUnwrapEither[A] =
    CanUnwrap.instance[Either[Throwable, *], A]((res, cb) => cb(res))
}
