package io.github.mkotsur.aws.handler
import io.circe.parser._
import io.circe.generic.auto._
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}

abstract class ProxyRequestHandler
  extends JsonHandler[ProxyRequest, ProxyResponse] {

  def handleRequest(req: ProxyRequest): ProxyResponse

  override final def handleJson(x: ProxyRequest): ProxyResponse = {
    val response = handleRequest(x)
    response.copy(
      headers = response.headers + ("Access-Control-Allow-Origin" -> "*")
    )
  }

}

