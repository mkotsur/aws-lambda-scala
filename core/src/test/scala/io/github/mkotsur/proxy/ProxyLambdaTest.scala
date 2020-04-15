package io.github.mkotsur.proxy

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import cats.syntax.either._
import com.amazonaws.services.lambda.runtime.Context
import io.circe.generic.auto._
import io.circe.parser._
import io.github.mkotsur.aws.eff.future.FutureLambda
import io.github.mkotsur.aws.handler.{Lambda, LambdaFailureException}
import io.github.mkotsur.aws.handler.Lambda._
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}
import org.mockito.MockitoSugar
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should

import scala.concurrent.Future
import scala.io.Source

object ProxyLambdaTest {

  class ProxyRawHandler extends Lambda.Proxy[String, String] {
    override def handle(input: ProxyRequest[String], c: Context) =
      Right(ProxyResponse(200, None, input.body.map(_.toUpperCase())))
  }

  class ProxyRawHandlerWithError extends Lambda.Proxy[String, String] {

    override def handle(i: ProxyRequest[String],
                        c: Context): Either[Throwable, ProxyResponse[String]] =
      Left(
        new Error("Could not handle this request for some obscure reasons")
      )
  }

  class ProxyCaseClassHandler extends Lambda.Proxy[Ping, Pong] {
    override def handle(input: ProxyRequest[Ping], c: Context) = Right(
      ProxyResponse(200, None, input.body.map { ping =>
        Pong(ping.inputMsg.length.toString)
      })
    )
  }

  class ProxyCaseClassHandlerWithError extends Lambda.Proxy[Ping, Pong] {
    override def handle(input: ProxyRequest[Ping], c: Context) = Left(
      new Error("Oh boy, something went wrong...")
    )
  }

  case class Ping(inputMsg: String)

  case class Pong(outputMsg: String)
}

class ProxyLambdaTest
    extends AnyFunSuite
    with should.Matchers
    with MockitoSugar
    with Eventually {

  import ProxyLambdaTest._
  private implicit def string2bytes(s: String): Array[Byte] = s.getBytes()

  test("should handle request and response classes with body of raw type") {

    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-raw.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new ByteArrayInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyRawHandler().handleRequest(is, os, mock[Context])

    os.toString should startWith("{")
    os.toString should include("RAW-BODY")
    os.toString should endWith("}")
  }

  test("should handle request and response classes with body of case classes") {
    val jsonUrl =
      getClass.getClassLoader.getResource("proxyInput-case-class.json")
    val s = Source.fromURL(jsonUrl)

    val is = new ByteArrayInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyCaseClassHandler().handleRequest(is, os, mock[Context])

    os.toString should startWith("{")
    os.toString should include("{\\\"outputMsg\\\":\\\"4\\\"}")
    os.toString should endWith("}")
  }

  test("should generate error response in case of error in raw handler") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-raw.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new ByteArrayInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    val context = mock[Context]
    when(context.getRemainingTimeInMillis).thenReturn(1000)

    val caught = intercept[LambdaFailureException] {
      new ProxyRawHandlerWithError().handleRequest(is, os, context)
    }

    caught.getCause.getMessage shouldEqual "Could not handle this request for some obscure reasons"

    val response = decode[ProxyResponse[String]](os.toString)
    response shouldEqual Right(
      ProxyResponse(
        500,
        Some(Map("Content-Type" -> s"text/plain; charset=UTF-8")),
        Some("Could not handle this request for some obscure reasons")
      ))
  }

  test("should generate error response in case of error in case class handler") {
    val jsonUrl =
      getClass.getClassLoader.getResource("proxyInput-case-class.json")
    val s = Source.fromURL(jsonUrl)

    val is = new ByteArrayInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    val caught = intercept[LambdaFailureException] {
      new ProxyCaseClassHandlerWithError().handleRequest(is, os, mock[Context])
    }

    caught.getCause.getMessage shouldEqual "Oh boy, something went wrong..."

    val response = decode[ProxyResponse[String]](os.toString)

    response shouldEqual Right(
      ProxyResponse(
        500,
        Some(Map("Content-Type" -> s"text/plain; charset=UTF-8")),
        Some("Oh boy, something went wrong...")
      ))
  }

  test("should support Future as output") {
    val jsonUrl =
      getClass.getClassLoader.getResource("proxyInput-case-class.json")
    val s = Source.fromURL(jsonUrl)

    val is = new ByteArrayInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    val context = mock[Context]
    when(context.getRemainingTimeInMillis).thenReturn(500 /*ms*/ )

    import Lambda.{canDecodeProxyRequest, canEncodeProxyResponse}

    new FutureLambda[ProxyRequest[Ping], ProxyResponse[Pong]] {
      override def handle(i: ProxyRequest[Ping], c: Context): Out =
        Future.successful(ProxyResponse.success(Some(Pong("4"))))
    }.handleRequest(is, os, context)

    eventually {
      os.toString should startWith("{")
      os.toString should include("{\\\"outputMsg\\\":\\\"4\\\"}")
      os.toString should endWith("}")
    }
  }

  test("should support failed Future as output") {
    val jsonUrl =
      getClass.getClassLoader.getResource("proxyInput-case-class.json")
    val s = Source.fromURL(jsonUrl)

    val is = new ByteArrayInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    val context = mock[Context]
    when(context.getRemainingTimeInMillis).thenReturn(500 /*ms*/ )

    //TODO: Make sure this import is not needed
    // and there is a convenience class for FutureProxy or smth
    import Lambda.{canDecodeProxyRequest, canEncodeProxyResponse}

    val thrown = intercept[LambdaFailureException] {
      new FutureLambda[ProxyRequest[Ping], ProxyResponse[String]] {
        override def handle(i: ProxyRequest[Ping], c: Context): Out =
          Future.failed[ProxyResponse[String]](new RuntimeException("Oops"))
      }.handleRequest(is, os, context)
    }

    thrown.getCause.getMessage shouldBe "Oops"

    eventually {
      val response = decode[ProxyResponse[String]](os.toString)
      response shouldEqual Either.right(
        ProxyResponse(
          500,
          Some(Map("Content-Type" -> s"text/plain; charset=UTF-8")),
          Some("Oops")
        ))
    }
  }

  test("should support returning Units") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-units.json")
    val s       = Source.fromURL(jsonUrl)

    val is = new ByteArrayInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    val context = mock[Context]
    when(context.getRemainingTimeInMillis).thenReturn(500 /*ms*/ )

    import Lambda.{canDecodeProxyRequest, canEncodeProxyResponse}

    new Lambda.Proxy[None.type, None.type] {
      override def handle(i: ProxyRequest[None.type], c: Context): Out = {
        val response = ProxyResponse[None.type](
          statusCode = 200,
          body = None
        )
        Either.right(response)
      }
    }.handleRequest(is, os, context)

    eventually {
      val response = decode[ProxyResponse[None.type]](os.toString)
      response shouldEqual Either.right(
        ProxyResponse(
          statusCode = 200,
          body = None
        ))
    }
  }

}
