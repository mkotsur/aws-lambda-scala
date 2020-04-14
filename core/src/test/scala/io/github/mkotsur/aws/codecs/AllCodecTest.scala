package io.github.mkotsur.aws.codecs

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.amazonaws.services.lambda.runtime.Context
import org.scalatest.EitherValues._
import org.scalatest.concurrent.Eventually
import org.mockito.MockitoSugar
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should

class AllCodecTest extends AnyFunSuite with should.Matchers with MockitoSugar with Eventually {

  private implicit def string2bytes(s: String): Array[Byte] = s.getBytes()

  test("should decode null") {
    new AllCodec {
      val is = new ByteArrayInputStream("""null""")

      val value = canDecodeAll[None.type].readStream(is)
      value.right.value shouldBe Option.empty[None.type]
    }
  }

  test("should decode empty string") {
    new AllCodec {
      val is = new ByteArrayInputStream("")

      val value = canDecodeAll[None.type].readStream(is)
      value.right.value shouldBe Option.empty[None.type]
    }
  }

  test("should encode null") {
    new AllCodec {
      val os = new ByteArrayOutputStream()

      val context: Context = mock[Context]

      canEncodeAll[None.type].writeStream(os, Right(None), context)
      os.toString shouldBe "null"
    }
  }

}
