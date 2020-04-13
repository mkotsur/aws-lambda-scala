package io.github

import java.nio.charset.StandardCharsets


package object mkotsur {

  import java.io.{ByteArrayInputStream, UnsupportedEncodingException}

  class StringInputStream @throws[UnsupportedEncodingException]
  (val string: String) extends ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)) {
    def getString: String = this.string
  }
}
