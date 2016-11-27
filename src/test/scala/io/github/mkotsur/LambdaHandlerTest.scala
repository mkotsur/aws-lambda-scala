package io.github.mkotsur

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.util.StringInputStream
import io.circe.generic.auto._
import io.github.mkotsur.aws.handler.LambdaHandler._
import io.github.mkotsur.LambdaHandlerTest._
import io.github.mkotsur.aws.handler.LambdaHandler
import org.scalatest._
import org.scalatest.mockito.MockitoSugar

// TODO: should fail to compile instead of giving wrong values
import io.github.mkotsur.aws.handler.LambdaHandler.string._

object LambdaHandlerTest {

  class PingPongHandler extends LambdaHandler[Ping, Pong] {
    override def handle(ping: Ping) = Right(Pong(ping.inputMsg.reverse))
  }

  class StringPongHandler extends LambdaHandler[String, Pong] {
    override def handle(input: String) = Right(Pong(input.toUpperCase()))
  }

  class PingStringHandler extends LambdaHandler[Ping, String] {
    override def handle(input: Ping) = Right(input.inputMsg.toLowerCase())
  }

  case class Ping(inputMsg: String)

  case class Pong(outputMsg: String)

}

class LambdaHandlerTest extends FunSuite with Matchers with MockitoSugar {

  test("should convert input/output to/from case classes") {

    val is = new StringInputStream("""{ "inputMsg": "hello" }""")
    val os = new ByteArrayOutputStream()

    new PingPongHandler().handle(is, os, mock[Context])

    os.toString shouldBe """{"outputMsg":"olleh"}"""
  }

  test("should allow to call 'handle()' via reflection") {
    val handlerClass = Class.forName(classOf[PingPongHandler].getName)
    val handlerMethod =   handlerClass.getMethod("handle", classOf[InputStream], classOf[OutputStream], classOf[Context])

    val handlerInstance = handlerClass.getConstructor().newInstance()

    val is = new StringInputStream("""{ "inputMsg": "hello" }""")
    val os = new ByteArrayOutputStream()

    handlerMethod.invoke(handlerInstance, is, os, mock[Context])

    os.toString shouldBe """{"outputMsg":"olleh"}"""
  }

  test("should allow to pass raw strings as input") {
    val is = new StringInputStream("hello")
    val os = new ByteArrayOutputStream()

    new StringPongHandler().handle(is, os, mock[Context])

    os.toString shouldBe """{"outputMsg":"HELLO"}"""
  }

  test("should allow to pass raw strings as output") {
    val is = new StringInputStream("""{ "inputMsg": "HeLLo" }""")
    val os = new ByteArrayOutputStream()

    new PingStringHandler().handle(is, os, mock[Context])

    os.toString shouldBe "hello"
  }

}
