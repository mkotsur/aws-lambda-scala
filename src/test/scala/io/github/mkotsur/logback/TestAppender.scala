package io.github.mkotsur.logback

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase

import scala.collection.mutable.ListBuffer

object TestAppender {
  val events = ListBuffer.empty[ILoggingEvent]
}

class TestAppender extends AppenderBase[ILoggingEvent] {

  override protected def append(e: ILoggingEvent) {
    TestAppender.events += e
  }
}