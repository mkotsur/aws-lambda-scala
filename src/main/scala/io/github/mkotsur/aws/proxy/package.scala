package io.github.mkotsur.aws

package object proxy {
  case class RequestContextAuthorizer(
      principalId: String
  )

  case class RequestContext(
      authorizer: Option[RequestContextAuthorizer] = None
  )

  object RequestContext {
    def withPrincipalId(id: String) = RequestContext(Some(RequestContextAuthorizer(id)))
  }

  case class RequestInput(body: String)

  case class ProxyRequest[T](
      path: String,
      httpMethod: String,
      headers: Option[Map[String, String]] = None,
      queryStringParameters: Option[Map[String, String]] = None,
      stageVariables: Option[Map[String, String]] = None,
      requestContext: RequestContext = RequestContext(),
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
