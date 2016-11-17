package io.github.mkotsur.aws

import io.circe.Encoder
import io.circe.syntax._

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

  case class ProxyRequest(path: String,
                          httpMethod: String,
                          headers: Map[String, String] = Map.empty,
                          queryStringParameters: Option[Map[String, String]] = None,
                          stageVariables: Option[Map[String, String]] = None,
                          body: Option[String] = None,
                          requestContext: RequestContext = RequestContext()
                         )

  case class ProxyResponse(
                            statusCode: Int,
                            headers: Map[String, String] = Map.empty,
                            body: Option[String] = None
                          )

  object ProxyResponse {

    def success[B](body: B)(implicit encoder: Encoder[B]): ProxyResponse = ProxyResponse(
      statusCode = 200,
      headers = Map("Access-Control-Allow-Origin" -> "*"),
      body = body match {
        case _: String => Some(body.toString)
        case _ => Some(body.asJson.noSpaces)
      }
    )
  }

}
