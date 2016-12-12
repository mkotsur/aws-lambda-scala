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

import scala.util.Try

object LambdaHandlerTest {

  class PingPongHandler extends LambdaHandler[Ping, Pong]() {
    override def handle(ping: Ping) = Right(Pong(ping.inputMsg.reverse))
  }

  class PingPongHandlerWithError extends LambdaHandler[Ping, Pong] {
    override def handle(ping: Ping) = Left(new Error("Oops"))
  }

  class StringPongHandler extends LambdaHandler[String, Pong] {
    override def handle(input: String) = Right(Pong(input.toUpperCase()))
  }

  class PingStringHandler extends LambdaHandler[Ping, String] {
    override def handle(input: Ping) = Right(input.inputMsg.toLowerCase())
  }

  class PingNothingHandler extends LambdaHandler[Ping, Unit] {
    override def handle(input: Ping) = Right()
  }

  class NothingPongHandler extends LambdaHandler[Unit, Pong] {
    override def handle(n: Unit) = Right(Pong("nothing"))
  }

  class SeqSeqHandler extends LambdaHandler[Seq[String], Seq[Int]] {
    override def handle(strings: Seq[String]) = Try {
      strings.map(_.toInt)
    }.toEither
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

  test("should throw an error if it happened in the handler") {
    val is = new StringInputStream("""{ "inputMsg": "HeLLo" }""")
    val os = new ByteArrayOutputStream()

    val caught = intercept[Error] {
      new PingPongHandlerWithError().handle(is, os, mock[Context])
    }

    caught.getMessage shouldEqual "Oops"
  }

  test("should support handlers of sequences") {
    val is = new StringInputStream("""["1","42"]""")
    val os = new ByteArrayOutputStream()

    new SeqSeqHandler().handle(is, os, mock[Context])

    os.toString shouldBe """[1,42]"""
    "".reverse
  }

}
