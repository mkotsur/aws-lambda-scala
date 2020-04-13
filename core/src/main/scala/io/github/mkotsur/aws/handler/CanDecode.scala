package io.github.mkotsur.aws.handler

import java.io.InputStream

import io.github.mkotsur.aws.handler.CanDecode.ReadStream

trait CanDecode[I] {
  def readStream: ReadStream[I]
}

object CanDecode {

  type ReadStream[I] = InputStream => Either[Throwable, I]

  def apply[A: CanDecode]: CanDecode[A] =
    implicitly[CanDecode[A]]

  def instance[A](func: ReadStream[A]): CanDecode[A] = new CanDecode[A] {
    override def readStream: ReadStream[A] = func
  }

}
