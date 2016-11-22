package io.github.mkotsur.aws.handler
import io.circe.generic.auto._
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}

abstract class ProxyRequestHandler
  extends LambdaHandler[ProxyRequest, ProxyResponse] {

  def handleRequest(req: ProxyRequest): ProxyResponse

  override final def handle(x: ProxyRequest): ProxyResponse = {
    val response = handleRequest(x)
    response.copy(
      headers = response.headers + ("Access-Control-Allow-Origin" -> "*")
    )
  }

}

