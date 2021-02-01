package io.github.mkotsur.aws.proxy

import better.files.Resource
import cats.implicits._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should
import io.circe.parser.decode

import java.util.Date
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder
import io.github.mkotsur.aws.handler.Lambda._

class ProxyRequestTest extends AnyFunSuite with should.Matchers with MockitoSugar with Eventually {

  case class MyClaims(token_use: String, auth_time: Long /*, exp: Date, iat: Date */ )
  case class MyAuthorizer(claims: MyClaims)
  case class MyContext(authorizer: MyAuthorizer)

  test("should deserialize JSON without Cognito user pool information") {

    val json          = Resource.getAsString("proxyInput-raw.json")
    val resultOrError = decode[ApiProxyRequest[String, Json]](json)

    val result = resultOrError.valueOr(throw _)

    result.body shouldBe "raw-body".some
    result.requestContext.asObject.map(_.contains("authorizer")) shouldBe Some(false)
  }

  /**
    * @see https://github.com/mkotsur/aws-lambda-scala/issues/24
    */
  test("should deserialize JSON with Cognito custom claims into a case class") {

    import io.circe.generic.auto._

    val json          = Resource.getAsString("proxyInput-claims.json")
    val resultOrError = decode[ApiProxyRequest[String, MyContext]](json)

    val result = resultOrError.valueOr(throw _)
    result.requestContext.authorizer.claims.token_use shouldBe "id"
    result.requestContext.authorizer.claims.auth_time shouldBe 1609775553
  }

}
