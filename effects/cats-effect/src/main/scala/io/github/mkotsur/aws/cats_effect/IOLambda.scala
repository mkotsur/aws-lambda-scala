package io.github.mkotsur.aws.cats_effect

import cats.effect.IO
import io.github.mkotsur.aws.handler.{CanDecode, CanEncode, FLambda}

abstract class IOLambda[I: CanDecode, O: CanEncode] extends FLambda[IO, I, O] {}
