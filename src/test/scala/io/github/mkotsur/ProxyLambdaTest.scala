package io.github.mkotsur

import java.io.ByteArrayOutputStream

import com.amazonaws.services.lambda.runtime.Context
import io.github.mkotsur.aws.handler.Lambda
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}
import io.circe.generic.auto._
import io.circe.parser._
import Lambda._
import ProxyLambdaTest._

import scala.io.Source

object ProxyLambdaTest {
  class ProxyRawHandler extends Lambda.Proxy[String, String] {
    override protected def handle(input: ProxyRequest[String]) = {
      Right(ProxyResponse(200, None, input.body.map(_.toUpperCase())))
    }
  }

  class ProxyRawHandlerWithError extends Lambda.Proxy[String, String] {

    override protected def handle(i: ProxyRequest[String]): Either[Throwable, ProxyResponse[String]] = Left(
      new Error("Could not handle this request for some obscure reasons")
    )
  }

  class ProxyCaseClassHandler extends Lambda.Proxy[Ping, Pong] {
    override protected def handle(input: ProxyRequest[Ping]) = Right(
      ProxyResponse(200, None, input.body.map { ping =>
        Pong(ping.inputMsg.length.toString)
      })
    )
  }

  class ProxyCaseClassHandlerWithError extends Lambda.Proxy[Ping, Pong] {
    override protected def handle(input: ProxyRequest[Ping]) = Left(
      new Error("Oh boy, something went wrong...")
    )
  }

  case class Ping(inputMsg: String)

  case class Pong(outputMsg: String)
}

class ProxyLambdaTest extends FunSuite with Matchers with MockitoSugar {

  test("should handle request and response classes with body of raw type") {

    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-raw.json")
    val s = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyRawHandler().handle(is, os, mock[Context])

    os.toString should startWith("{")
    os.toString should include("RAW-BODY")
    os.toString should endWith("}")
  }

  test("should handle request and response classes with body of case classes") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-case-class.json")
    val s = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyCaseClassHandler().handle(is, os, mock[Context])

    os.toString should startWith("{")
    os.toString should include("{\\\"outputMsg\\\":\\\"4\\\"}")
    os.toString should endWith("}")
  }

  test("should generate error response in case of error in raw handler") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-raw.json")
    val s = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyRawHandlerWithError().handle(is, os, mock[Context])

    val response = decode[ProxyResponse[String]](os.toString)
    response shouldEqual Right(ProxyResponse(
      500,
      Some(Map("Content-Type" -> s"text/plain; charset=UTF-8")),
      Some("Could not handle this request for some obscure reasons")
    ))
  }

  test("should generate error response in case of error in case class handler") {
    val jsonUrl = getClass.getClassLoader.getResource("proxyInput-case-class.json")
    val s = Source.fromURL(jsonUrl)

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyCaseClassHandlerWithError().handle(is, os, mock[Context])

    val response = decode[ProxyResponse[String]](os.toString)

    response shouldEqual Right(ProxyResponse(
      500,
      Some(Map("Content-Type" -> s"text/plain; charset=UTF-8")),
      Some("Oh boy, something went wrong...")
    ))
  }

}

