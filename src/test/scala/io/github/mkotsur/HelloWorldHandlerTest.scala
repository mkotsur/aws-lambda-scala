package io.github.mkotsur

import java.io.ByteArrayOutputStream

import com.amazonaws.util.StringInputStream
import org.scalatest._

class HelloWorldHandlerTest extends FunSuite with ShouldMatchers {

  test("testHandle") {

    val input = """{ "msg": "hello" }"""

    val baos: ByteArrayOutputStream = new ByteArrayOutputStream()

    new HelloWorldHandler().handleInternal(new StringInputStream(input), baos, null)

    baos.toString shouldBe """{"msg":"olleh"}"""
  }

}
