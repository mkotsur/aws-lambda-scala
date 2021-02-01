package io.github.mkotsur.aws

import io.circe.{Decoder}

package object proxy {
  case class RequestContextAuthorizer(
      principalId: String
  )

  case class RequestContext(
      authorizer: Option[RequestContextAuthorizer]
  )

  case class RequestInput(body: String)

  case class ApiProxyRequest[B, C: Decoder](
      path: String,
      pathParameters: Option[Map[String, String]] = None,
      httpMethod: String,
      headers: Option[Map[String, String]] = None,
      queryStringParameters: Option[Map[String, String]] = None,
      stageVariables: Option[Map[String, String]] = None,
      requestContext: C,
      body: Option[B] = None
  )

  case class ApiProxyResponse[T](
      statusCode: Int,
      headers: Option[Map[String, String]] = None,
      body: Option[T] = None
  )

  object ApiProxyResponse {

    def success[B](body: Option[B] = None): ApiProxyResponse[B] = ApiProxyResponse[B](
      statusCode = 200,
      headers = Some(Map("Access-Control-Allow-Origin" -> "*")),
      body = body
    )
  }

}
