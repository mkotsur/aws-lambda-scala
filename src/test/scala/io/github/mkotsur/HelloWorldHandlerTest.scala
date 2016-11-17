package io.github.mkotsur

import java.io.ByteArrayOutputStream

import com.amazonaws.util.StringInputStream
import io.github.mkotsur.HelloWorldHandlerTest.HelloWorldHandler
import io.github.mkotsur.aws.handler.JsonHandler
import org.scalatest._

object HelloWorldHandlerTest {

  import io.circe.generic.auto._

  class HelloWorldHandler extends JsonHandler[Ping, Pong] {
    override def handleJson(ping: Ping): Pong = Pong(ping.msg.reverse)
  }

  case class Ping(msg: String)

  case class Pong(msg: String)

}

class HelloWorldHandlerTest extends FunSuite with Matchers {

  test("testHandle") {

    val input = """{ "msg": "hello" }"""

    val baos: ByteArrayOutputStream = new ByteArrayOutputStream()

    new HelloWorldHandler().handle(new StringInputStream(input), baos, null)

    baos.toString shouldBe """{"msg":"olleh"}"""
  }

}
