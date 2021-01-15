package io.github.mkotsur.aws

import io.circe.{Decoder, Json}

package object proxy {
  case class RequestContextAuthorizer(
      principalId: String
  )

  case class RequestContext(
      authorizer: Option[RequestContextAuthorizer]
  )

  case class RequestInput(body: String)

  type ProxyRequest[T] = ApiProxyRequest[T, Json]

  case class ApiProxyRequest[T, C: Decoder](
      path: String,
      pathParameters: Option[Map[String, String]] = None,
      httpMethod: String,
      headers: Option[Map[String, String]] = None,
      queryStringParameters: Option[Map[String, String]] = None,
      stageVariables: Option[Map[String, String]] = None,
      requestContext: C,
      body: Option[T] = None
  )

  case class ProxyResponse[T](
      statusCode: Int,
      headers: Option[Map[String, String]] = None,
      body: Option[T] = None
  )

  object ProxyResponse {

    def success[B](body: Option[B] = None): ProxyResponse[B] = ProxyResponse[B](
      statusCode = 200,
      headers = Some(Map("Access-Control-Allow-Origin" -> "*")),
      body = body
    )
  }

}
