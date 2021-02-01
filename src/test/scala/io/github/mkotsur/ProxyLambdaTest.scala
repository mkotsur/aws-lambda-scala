package io.github.mkotsur

import java.io.ByteArrayOutputStream
import cats.syntax.either._
import com.amazonaws.services.lambda.runtime.Context
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import io.github.mkotsur.ProxyLambdaTest._
import io.github.mkotsur.aws.handler.Lambda
import io.github.mkotsur.aws.handler.Lambda._
import io.github.mkotsur.aws.proxy.{ApiProxyRequest, ApiProxyResponse, RequestContext}
import org.scalatest.concurrent.Eventually
import org.mockito.MockitoSugar
import org.scalatest.matchers.should
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.Future
import scala.io.Source

object ProxyLambdaTest {

  class ProxyRawHandler extends Lambda.ApiProxy[String, RequestContext, String] {
    override protected def handle(input: ApiProxyRequest[String, RequestContext]) =
      Right(ApiProxyResponse(200, None, input.body.map(_.toUpperCase())))
  }

  class ProxyRawHandlerWithError extends Lambda.ApiProxy[String, RequestContext, String] {

    override protected def handle(
        i: ApiProxyRequest[String, RequestContext]): Either[Throwable, ApiProxyResponse[String]] =
      Left(
        new Error("Could not handle this request for some obscure reasons")
      )
  }

  class ProxyCaseClassHandler extends Lambda.ApiProxy[Ping, RequestContext, Pong] {
    override protected def handle(input: ApiProxyRequest[Ping, RequestContext]) = Right(
      ApiProxyResponse(200, None, input.body.map { ping =>
        Pong(ping.inputMsg.length.toString)
      })
    )
  }

  class ProxyCaseClassHandlerWithError extends Lambda.ApiProxy[Ping, RequestContext, Pong] {
    override protected def handle(input: ApiProxyRequest[Ping, RequestContext]) = Left(
      new Error("Oh boy, something went wrong...")
    )
  }

  case class Ping(inputMsg: String)

  case class Pong(outputMsg: String)
}

class ProxyLambdaTest extends AnyFunSuite with should.Matchers with MockitoSugar with Eventually {

  test("should handle request and response classes with body of raw type") {

    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-raw.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyRawHandler().handle(is, os, mock[Context])

    os.toString should startWith("{")
    os.toString should include("RAW-BODY")
    os.toString should endWith("}")
  }

  test("should handle request and response classes with body of case classes") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-case-class.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyCaseClassHandler().handle(is, os, mock[Context])

    os.toString should startWith("{")
    os.toString should include("{\\\"outputMsg\\\":\\\"4\\\"}")
    os.toString should endWith("}")
  }

  test("should generate error response in case of error in raw handler") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-raw.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyRawHandlerWithError().handle(is, os, mock[Context])

    val response = decode[ApiProxyResponse[String]](os.toString)
    response shouldEqual Right(
      ApiProxyResponse(
        500,
        Some(Map("Content-Type" -> s"text/plain; charset=UTF-8")),
        Some("Could not handle this request for some obscure reasons")
      ))
  }

  test("should generate error response in case of error in case class handler") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-case-class.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyCaseClassHandlerWithError().handle(is, os, mock[Context])

    val response = decode[ApiProxyResponse[String]](os.toString)

    response shouldEqual Right(
      ApiProxyResponse(
        500,
        Some(Map("Content-Type" -> s"text/plain; charset=UTF-8")),
        Some("Oh boy, something went wrong...")
      ))
  }

  test("should support Future as output") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-case-class.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    val context = mock[Context]
    when(context.getRemainingTimeInMillis).thenReturn(500 /*ms*/ )

    import Lambda.{canDecodeProxyRequest, canEncodeFuture, canEncodeProxyResponse}

    val function
      : (ApiProxyRequest[Ping, RequestContext], Context) => Either[Throwable, ApiProxyResponse[Future[Pong]]] =
      (_: ApiProxyRequest[Ping, RequestContext], _) =>
        Right(ApiProxyResponse.success(Some(Future.successful(Pong("4")))))
    Lambda.Proxy.instance(function).handle(is, os, context)

    eventually {
      os.toString should startWith("{")
      os.toString should include("{\\\"outputMsg\\\":\\\"4\\\"}")
      os.toString should endWith("}")
    }
  }

  test("should support failed Future as output") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-case-class.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    val context = mock[Context]
    when(context.getRemainingTimeInMillis).thenReturn(500 /*ms*/ )

    import Lambda.{canDecodeProxyRequest, canEncodeFuture, canEncodeProxyResponse}

    Lambda.Proxy
      .instance((_: ApiProxyRequest[Ping, RequestContext], _: Context) => {
        val response = ApiProxyResponse.success(Some(Future.failed[String](new RuntimeException("Oops"))))
        Either.right(response)
      })
      .handle(is, os, context)

    eventually {
      val response = decode[ApiProxyResponse[String]](os.toString)
      response shouldEqual Either.right(
        ApiProxyResponse(
          500,
          Some(Map("Content-Type" -> s"text/plain; charset=UTF-8")),
          Some("Oops")
        ))
    }
  }

  test("should support returning Units") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-units.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    val context = mock[Context]
    when(context.getRemainingTimeInMillis).thenReturn(500 /*ms*/ )

    import Lambda.{canDecodeProxyRequest, canEncodeProxyResponse}

    Lambda.Proxy
      .instance[None.type, RequestContext, None.type]((_, _) => {
        val response = ApiProxyResponse[None.type](
          statusCode = 200,
          body = None
        )
        Either.right(response)
      })
      .handle(is, os, context)

    eventually {
      val response = decode[ApiProxyResponse[None.type]](os.toString)
      response shouldEqual Either.right(
        ApiProxyResponse(
          statusCode = 200,
          body = None
        ))
    }
  }

}
