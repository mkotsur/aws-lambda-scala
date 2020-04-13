package io.github.mkotsur.aws.cats_effect

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase

import scala.collection.mutable.ListBuffer

//TODO: we ned something better than this test appender as it's stateful
//      and awful.
object TestAppender {
  val events = ListBuffer.empty[ILoggingEvent]
}

class TestAppender extends AppenderBase[ILoggingEvent] {

  override protected def append(e: ILoggingEvent): Unit =
    TestAppender.events += e
}
