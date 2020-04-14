package io.github.mkotsur.handler

import org.mockito.MockitoSugar
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should

class LambdaTest extends AnyFunSuite with should.Matchers with MockitoSugar with OptionValues with Eventually {

  test("should compile") {
    """
    import io.github.mkotsur.aws.handler.Lambda
    import io.github.mkotsur.aws.handler.Lambda._

    class MyHandler extends Lambda[String, String] {}
    """ should compile
  }

}
