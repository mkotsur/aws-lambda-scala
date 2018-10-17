package io.github.mkotsur.aws.codecs

import java.io.ByteArrayOutputStream

import com.amazonaws.services.lambda.runtime.Context
import io.circe.generic.auto._
import io.github.mkotsur.StringInputStream
import org.scalatest.EitherValues._
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

class AllCodecTest extends FunSuite with Matchers with MockitoSugar with Eventually {

  test("should decode null") {
    new AllCodec {
      val is = new StringInputStream("""null""")

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
