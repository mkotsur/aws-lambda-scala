package io.github.mkotsur.handler

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.util.concurrent.TimeoutException

import com.amazonaws.services.lambda.runtime.Context
import io.circe.generic.auto._
import io.github.mkotsur.aws.eff.either.ThrowableOr
import io.github.mkotsur.aws.handler.Lambda._
import io.github.mkotsur.aws.handler.{FLambda, Lambda}
import org.mockito.MockitoSugar
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object FLambdaTest {

  import io.github.mkotsur.aws.eff.either.canUnwrapEither

  private class PingPong extends FLambda[ThrowableOr, Ping, Pong]() {
    override def handle(ping: Ping, c: Context): Out = Right(Pong(ping.inputMsg.reverse))
  }

  private class PingPongWithError extends FLambda[ThrowableOr, Ping, Pong] {
    override def handle(ping: Ping, c: Context) = Left(new Error("PingPongWithError: Oops"))
  }

  private class PingPongThrowingAnError extends FLambda[ThrowableOr, Ping, Pong] {
    override def handle(ping: Ping, c: Context) = throw new Error("PingPongThrowingAnError: Oops")
  }

  private class StringPong extends FLambda[ThrowableOr, String, Pong] {
    override def handle(input: String, c: Context) = Right(Pong(input.toUpperCase()))
  }

  private class PingString extends FLambda[ThrowableOr, Ping, String] {
    override def handle(input: Ping, c: Context) = Right(input.inputMsg.toLowerCase())
  }

  private class PingNothing extends FLambda[ThrowableOr, Ping, Unit] {
    override def handle(input: Ping, c: Context) = Right(())
  }

  private class NothingPong extends FLambda[ThrowableOr, Unit, Pong] {
    override def handle(n: Unit, c: Context) = Right(Pong("nothing"))
  }

  private class SeqSeq extends FLambda[ThrowableOr, Seq[String], Seq[Int]] {
    override def handle(strings: Seq[String], c: Context): Either[Throwable, Seq[Int]] =
      Try {
        strings.map(_.toInt)
      } match {
        case Success(v)  => Right(v)
        case Failure(ex) => Left(ex)
      }
  }

  private class OptionOption extends FLambda[ThrowableOr, Option[Ping], Option[Pong]] {
    override def handle(input: Option[Ping], c: Context) = Right(
      input.map { ping =>
        Pong(ping.inputMsg.length.toString)
      }
    )
  }

  private case class Ping(inputMsg: String)

  private case class Pong(outputMsg: String)

}

class FLambdaTest extends AnyFunSuite with should.Matchers with MockitoSugar with OptionValues with Eventually {

  import FLambdaTest._

  private implicit def string2bytes(s: String): Array[Byte] = s.getBytes()

  test("should convert input/output to/from case classes") {

    val is = new ByteArrayInputStream("""{ "inputMsg": "hello" }""")
    val os = new ByteArrayOutputStream()

    new PingPong().handleRequest(is, os, mock[Context])

    os.toString shouldBe """{"outputMsg":"olleh"}"""
  }

  test("should allow to call 'handle()' via reflection") {
    val handlerClass = Class.forName(classOf[PingPong].getName)
    val handlerMethod =
      handlerClass.getMethod("handleRequest", classOf[InputStream], classOf[OutputStream], classOf[Context])

    val handlerInstance = handlerClass.getConstructor().newInstance()

    val is = new ByteArrayInputStream("""{ "inputMsg": "hello" }""")
    val os = new ByteArrayOutputStream()

    handlerMethod.invoke(handlerInstance, is, os, mock[Context])

    os.toString shouldBe """{"outputMsg":"olleh"}"""
  }

  test("should allow to pass raw strings as input") {
    val is = new ByteArrayInputStream("hello")
    val os = new ByteArrayOutputStream()

    new StringPong().handleRequest(is, os, mock[Context])

    os.toString shouldBe """{"outputMsg":"HELLO"}"""
  }

  test("should allow to pass raw strings as output") {
    val is = new ByteArrayInputStream("""{ "inputMsg": "HeLLo" }""")
    val os = new ByteArrayOutputStream()

    new PingString().handleRequest(is, os, mock[Context])

    os.toString shouldBe "hello"
  }

  test("should rethrow an error if it has been thrown in the handler") {
    val is = new ByteArrayInputStream("""{ "inputMsg": "HeLLo" }""")
    val os = new ByteArrayOutputStream()

    val caught = intercept[Error] {
      new PingPongThrowingAnError().handleRequest(is, os, mock[Context])
    }

    caught.getMessage shouldEqual "Uncaught error while executing lambda handler"
  }

  test("should re-throw an error if the handler returned Left") {
    val is = new ByteArrayInputStream("""{ "inputMsg": "HeLLo" }""")
    val os = new ByteArrayOutputStream()

    val caught = intercept[Error] {
      new PingPongWithError().handleRequest(is, os, mock[Context])
    }

    caught.getMessage shouldEqual "PingPongWithError: Oops"
  }

  test("should support handlers of sequences") {
    val is = new ByteArrayInputStream("""["1","42"]""")
    val os = new ByteArrayOutputStream()

    new SeqSeq().handleRequest(is, os, mock[Context])

    os.toString shouldBe """[1,42]"""
    "".reverse
  }

  test("should inject context when overriding the appropriate method") {
    val handler = new Lambda[Int, String] {
      override def handle(input: Int, context: Context): Either[Throwable, String] =
        Right(s"${context.getFunctionName}: $input")
    }

    val is = new ByteArrayInputStream("42")
    val os = new ByteArrayOutputStream()

    val contextMock = mock[Context]
    when(contextMock.getFunctionName).thenReturn("testFunction")

    handler.handleRequest(is, os, contextMock)

    os.toString shouldBe "testFunction: 42"
  }

  test("should support None as input and output") {
    val is = new ByteArrayInputStream("null")
    val os = new ByteArrayOutputStream()

    new OptionOption().handleRequest(is, os, mock[Context])

    os.toString should be("null")
  }

  test("should support Some as input and output") {
    val is = new ByteArrayInputStream("""{ "inputMsg": "HeLLo" }""")
    val os = new ByteArrayOutputStream()

    new OptionOption().handleRequest(is, os, mock[Context])

    os.toString should be("""{"outputMsg":"5"}""")
  }

  test("should support Future as output") {
    val is = new ByteArrayInputStream("""{ "inputMsg": "hello" }""")
    val os = new ByteArrayOutputStream()

    val handler = new Lambda[Ping, Future[Pong]] {
      override def handle(ping: Ping, c: Context): Out = Right(Future.successful(Pong(ping.inputMsg.reverse)))
    }
    handler.handleRequest(is, os, mock[Context])

    eventually {
      os.toString shouldBe """{"outputMsg":"olleh"}"""
    }
  }

  test("should support failed Future as output") {
    val is = new ByteArrayInputStream("""{ "inputMsg": "hello" }""")
    val os = new ByteArrayOutputStream()

    val handler = new Lambda[Ping, Future[Pong]] {
      override def handle(i: Ping, c: Context): Out =
        Right(Future.failed(new IndexOutOfBoundsException("Something is wrong")))
    }

    an[IndexOutOfBoundsException] should be thrownBy handler.handleRequest(is, os, mock[Context])
  }

  test("should fail when Future takes longer than the execution context is ready to provide") {
    import scala.concurrent.ExecutionContext.Implicits.global
    val is = new ByteArrayInputStream("""{ "inputMsg": "hello" }""")
    val os = new ByteArrayOutputStream()

    val context = mock[Context]
    when(context.getRemainingTimeInMillis).thenReturn(500 /*ms*/ )

    val handler = new Lambda[Ping, Future[Pong]] {
      override def handle(i: Ping, c: Context): Out =
        Right(Future {
          Thread.sleep(1000)
          Pong("Not gonna happen")
        })
    }

    an[TimeoutException] should be thrownBy handler.handleRequest(is, os, context)
  }

  test("should do side effects only") {
    var done    = false
    val is      = new ByteArrayInputStream("""null""")
    val os      = new ByteArrayOutputStream()
    val context = mock[Context]

    new Lambda[None.type, None.type] {
      override def handle(i: None.type, c: Context): Out = Right(None)
    }.handleRequest(is, os, context)

    done shouldBe true
  }
}
