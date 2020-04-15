package io.github.mkotsur.aws.eff

import io.github.mkotsur.aws.handler.{
  CanDecode,
  CanEncode,
  CanUnwrap,
  FLambda,
  Lambda
}
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}

import scala.concurrent.Future

package object future {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit def canUnwrapFuture[A] =
    CanUnwrap.instance[Future, A]((res, cb) =>
      res.onComplete(resultTry => cb(resultTry.toEither)))

  // We use the abstract class here, because
  // we can do the "binding" of the implicit instance
  // of the [[CanUnwrap]] typeclass.
  // Therefor, Lambda can not be declared just as a type.
  abstract class FutureLambda[I: CanDecode, O: CanEncode]
      extends FLambda[Future, I, O]

}
