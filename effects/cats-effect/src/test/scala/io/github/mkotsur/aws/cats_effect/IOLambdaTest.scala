package io.github.mkotsur.aws.cats_effect

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import cats.effect.IO
import cats.implicits._
import ch.qos.logback.classic.Level
import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger}
import io.circe.generic.auto._
import io.github.mkotsur.aws.handler.Lambda._
import org.mockito.MockitoSugar
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should

object IOLambdaTest {

  case class Ping(inputMsg: String)

  case class Pong(outputMsg: String)

  class IOSuccessObj extends IOLambda[Ping, Pong] {
    override def handle(ping: Ping, c: Context): Out = IO.pure(Pong(ping.inputMsg.reverse))
  }

  class IOSuccessScalar extends IOLambda[Int, Int] {
    override def handle(i: Int, c: Context): Out =
      IO.pure(i * 2)
  }

  class IOSuccessLog extends IOLambda[Int, Int] {
    override def handle(i: Int, c: Context): Out =
      IO(c.getLogger.log("Greetings from the lambda")) >>
        IO.pure(i * 2)
  }

  class IOFailure extends IOLambda[Unit, Unit] {
    override def handle(i: Unit, c: Context): Out =
      IO.raiseError(new RuntimeException("Oops"))
  }

  class WildFailure extends IOLambda[Unit, Unit] {
    override def handle(i: Unit, c: Context): Out =
      throw new RuntimeException("Oops")
  }

}

class IOLambdaTest extends AnyFunSuite with should.Matchers with MockitoSugar with OptionValues with Eventually {

  import IOLambdaTest._

  test("should work for case classes") {

    val is = new ByteArrayInputStream("""{ "inputMsg": "hello" }""".getBytes())
    val os = new ByteArrayOutputStream()

    new IOSuccessObj().handleRequest(is, os, mock[Context])

    os.toString shouldBe """{"outputMsg":"olleh"}"""
  }

  test("work for scalars") {
    val is = new ByteArrayInputStream("42".getBytes())
    val os = new ByteArrayOutputStream()

    new IOSuccessScalar().handleRequest(is, os, mock[Context])

    os.toString shouldBe "84"
  }

  test("can log via context") {
    val is = new ByteArrayInputStream("2".getBytes())
    val os = new ByteArrayOutputStream()

    val context = mock[Context]
    val logger  = mock[LambdaLogger]
    when(context.getLogger).thenReturn(logger)

    new IOSuccessLog().handleRequest(is, os, context)

    verify(logger).log("Greetings from the lambda")
    os.toString shouldBe "4"
  }

  test("work for IO failures") {
    val is = new ByteArrayInputStream(Array.emptyByteArray)
    val os = new ByteArrayOutputStream()

    new IOFailure().handleRequest(is, os, mock[Context])

    TestAppender.events should have length 1

    val loggingEvent = TestAppender.events.headOption.value
    loggingEvent.getMessage should include("Lambda handler returned a failure-value")
    loggingEvent.getLevel shouldBe Level.ERROR
  }

  test("should log side-effectful exceptions") {
    val is = new ByteArrayInputStream(Array.emptyByteArray)
    val os = new ByteArrayOutputStream()

    val caught = intercept[Error] {
      new WildFailure().handleRequest(is, os, mock[Context])
    }

    caught.getMessage shouldEqual "Uncaught error while executing lambda handler"

    // We assume that the error has been logged by the handler itself in this case
    // Hence the log should either be empty, or contain something different
    //TODO: fix this check
//    TestAppender.events should have length 0
  }
}
