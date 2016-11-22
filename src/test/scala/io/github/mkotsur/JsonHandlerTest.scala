package io.github.mkotsur

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.util.StringInputStream
import io.github.mkotsur.JsonHandlerTest.{PingPongHandler, PingStringHandler, StringPongHandler}
import io.github.mkotsur.aws.handler.JsonHandler
import org.scalatest._

object JsonHandlerTest {

  import io.circe.generic.auto._

  class PingPongHandler extends JsonHandler[Ping, Pong] {
    override def handleJson(ping: Ping): Pong = Pong(ping.inputMsg.reverse)
  }

  class StringPongHandler extends JsonHandler[String, Pong] {
    override def handleJson(input: String): Pong = Pong(input.toUpperCase())
  }

  class PingStringHandler extends JsonHandler[Ping, String] {
    override def handleJson(input: Ping): String = input.inputMsg.toLowerCase()
  }

  case class Ping(inputMsg: String)

  case class Pong(outputMsg: String)

}

class JsonHandlerTest extends FunSuite with Matchers {

  test("should convert input/output to/from case classes") {

    val is = new StringInputStream("""{ "inputMsg": "hello" }""")
    val os = new ByteArrayOutputStream()

    new PingPongHandler().handle(is, os, null)

    os.toString shouldBe """{"outputMsg":"olleh"}"""
  }

  test("should allow to call 'handle()' via reflection") {
    val handlerClass = Class.forName(classOf[PingPongHandler].getName)
    val handlerMethod =   handlerClass.getMethod("handle", classOf[InputStream], classOf[OutputStream], classOf[Context])

    val handlerInstance = handlerClass.getConstructors.head.newInstance()

    val is = new StringInputStream("""{ "inputMsg": "hello" }""")
    val os = new ByteArrayOutputStream()

    handlerMethod.invoke(handlerInstance, is, os, null)

    os.toString shouldBe """{"outputMsg":"olleh"}"""
  }

  test("should allow to pass raw strings as input") {
    val is = new StringInputStream("hello")
    val os = new ByteArrayOutputStream()

    new StringPongHandler().handle(is, os, null)

    os.toString shouldBe """{"outputMsg":"HELLO"}"""
  }

  test("should allow to pass raw strings as output") {
    val is = new StringInputStream("""{ "inputMsg": "HeLLo" }""")
    val os = new ByteArrayOutputStream()

    new PingStringHandler().handle(is, os, null)

    os.toString shouldBe "hello"
  }

}
