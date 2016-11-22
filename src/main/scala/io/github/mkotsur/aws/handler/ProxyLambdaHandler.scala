package io.github.mkotsur.aws.handler

import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}

abstract class ProxyLambdaHandler[I, O](implicit decoder: Decoder[I], encoder: Encoder[O]) extends
  LambdaHandler[ProxyRequest, ProxyResponse] {

  def handleProxyJson(x: I, proxyRequest: ProxyRequest): O

  def requestToJson(req: ProxyRequest): I = {
    val body: String = req.body.getOrElse(throw new RuntimeException("No body detected in the request"))
    decode[I](body) match {
      case Left(e) => throw e
      case Right(input) => input
    }
  }

  override def handle(x: ProxyRequest): ProxyResponse =
    ProxyResponse.success(handleProxyJson(requestToJson(x), x))
}
