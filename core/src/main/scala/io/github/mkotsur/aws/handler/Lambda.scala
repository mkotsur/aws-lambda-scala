package io.github.mkotsur.aws.handler

import io.github.mkotsur.aws.codecs._
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}

import scala.language.postfixOps

object Lambda extends AllCodec with ProxyRequestCodec {

  type Proxy[I, O] = Lambda[ProxyRequest[I], ProxyResponse[O]]

}

// We use the abstract class here, because
// we can do the "binding" of the implicit instance
// of the [[CanUnwrap]] typeclass.
// Therefor, Lambda can not be declared just as a type.
import io.github.mkotsur.aws.eff.either._
abstract class Lambda[I: CanDecode, O: CanEncode] extends FLambda[ThrowableOr, I, O]
