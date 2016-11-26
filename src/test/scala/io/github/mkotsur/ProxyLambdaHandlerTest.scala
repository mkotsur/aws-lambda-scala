package io.github.mkotsur

import java.io.ByteArrayOutputStream

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.util.StringInputStream
import io.github.mkotsur.aws.handler.LambdaHandler
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.io.Source

object ProxyLambdaHandlerTest {
  object raw {
    import LambdaHandler._
    import io.circe.generic.auto._

    class ProxyRawHandler extends LambdaHandler[ProxyRequest[String], ProxyResponse[String]] {
      override protected def handle(input: ProxyRequest[String]) = {
        Right(ProxyResponse(200, None, input.body.map(_.toUpperCase())))
      }
    }

  }

  object caseclass {
    import LambdaHandler.proxy.canDecodeProxyRequest
    import io.circe.generic.auto._

    class ProxyCaseClassHandler extends LambdaHandler[ProxyRequest[Ping], ProxyResponse[Pong]] {
      override protected def handle(input: ProxyRequest[Ping]) = Right(
        ProxyResponse(200, None, input.body.map { ping =>
          Pong(ping.inputMsg.length.toString)
        })
      )
    }
  }


  case class Ping(inputMsg: String)

  case class Pong(outputMsg: String)
}

class ProxyLambdaHandlerTest extends FunSuite with Matchers with MockitoSugar {

  test("should handle request and response classes with body of raw type") {
    import ProxyLambdaHandlerTest.raw._

    val s = Source.fromResource("proxyInput-raw.json")

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyRawHandler().handle(is, os, mock[Context])

    os.toString should startWith("{")
    os.toString should include("RAW-BODY")
    os.toString should endWith("}")
  }

  test("should handle request and response classes with body of case classes") {
    import ProxyLambdaHandlerTest.caseclass._

    val s = Source.fromResource("proxyInput-case-class.json")

    val is = new StringInputStream(s.mkString)
    val os = new ByteArrayOutputStream()

    new ProxyCaseClassHandler().handle(is, os, mock[Context])

    os.toString should startWith("{")
    os.toString should include("\"outputMsg\":\"4\"")
    os.toString should endWith("}")
  }

}

